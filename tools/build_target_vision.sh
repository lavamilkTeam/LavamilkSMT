#!/usr/bin/env bash
# 在已准备好 /opt/smt/python、venv、系统编译依赖和源码包的目标板运行。
# 不替换系统 Python；完整前置条件见 docs/target-environment.md。
set -euo pipefail

task_prefix=/opt/smt
task_python="$task_prefix/venv/bin/python"
task_build_jobs=${SMT_BUILD_JOBS:-3}
task_compiler_launcher=${SMT_COMPILER_LAUNCHER:-}
export PATH="$task_prefix/venv/bin:$PATH"
export TMPDIR="$task_prefix/build/tmp"
export OPENBLAS_NUM_THREADS=1
mkdir -p "$TMPDIR" "$task_prefix/wheels"

cd "$task_prefix/setup"
printf '%s\n' \
  '2a02aba9ed12e4ac4eb3ea9421c420301a0c6460d9830d74a9df87efa4912010  numpy-1.26.4.tar.gz' \
  '1d40ca017ea51c533cf9fd5cbde5b5fe7ae248291ddf2af99d4c17cf8e13017d  opencv-4.13.0.tar.gz' \
  | sha256sum -c -

if ! "$task_python" -c 'import numpy; assert numpy.__version__ == "1.26.4"' 2>/dev/null; then
  "$task_python" -m pip wheel --no-deps --no-build-isolation \
    --config-settings=compile-args=-j2 \
    "$task_prefix/setup/numpy-1.26.4.tar.gz" -w "$task_prefix/wheels"
  "$task_python" -m pip install --no-index --find-links "$task_prefix/wheels" numpy==1.26.4
fi

if [[ ! -d "$task_prefix/build/opencv-4.13.0" ]]; then
  tar -xzf opencv-4.13.0.tar.gz -C "$task_prefix/build"
fi

task_numpy_include=$("$task_python" -c 'import numpy; print(numpy.get_include())')
cmake -S "$task_prefix/build/opencv-4.13.0" -B "$task_prefix/build/opencv-build" -G Ninja \
  -DCMAKE_BUILD_TYPE=Release \
  -DCMAKE_INSTALL_PREFIX="$task_prefix/opencv" \
  -DCMAKE_INSTALL_RPATH="$task_prefix/opencv/lib" \
  -DCMAKE_C_COMPILER_LAUNCHER="$task_compiler_launcher" \
  -DCMAKE_CXX_COMPILER_LAUNCHER="$task_compiler_launcher" \
  -DBUILD_LIST=core,imgproc,imgcodecs,videoio,calib3d,python3 \
  -DBUILD_SHARED_LIBS=ON \
  -DBUILD_TESTS=OFF -DBUILD_PERF_TESTS=OFF -DBUILD_EXAMPLES=OFF \
  -DBUILD_JAVA=OFF -DBUILD_opencv_apps=OFF -DBUILD_opencv_python2=OFF \
  -DBUILD_opencv_python3=ON \
  -DWITH_GTK=OFF -DWITH_QT=OFF -DWITH_OPENCL=OFF -DWITH_OPENGL=OFF \
  -DWITH_FFMPEG=OFF -DWITH_GSTREAMER=OFF -DWITH_V4L=ON \
  -DWITH_IPP=OFF -DWITH_ITT=OFF -DWITH_PROTOBUF=OFF \
  -DWITH_OPENEXR=OFF -DWITH_JASPER=OFF -DWITH_OPENJPEG=OFF \
  -DWITH_TIFF=OFF -DWITH_WEBP=OFF -DWITH_JPEG=ON -DWITH_PNG=ON \
  -DCPU_BASELINE=NEON -DCPU_DISPATCH= \
  -DPYTHON3_EXECUTABLE="$task_python" \
  -DPYTHON3_INCLUDE_DIR="$task_prefix/python/include/python3.11" \
  -DPYTHON3_LIBRARY="$task_prefix/python/lib/libpython3.11.so" \
  -DPYTHON3_NUMPY_INCLUDE_DIRS="$task_numpy_include" \
  -DOPENCV_PYTHON3_INSTALL_PATH="$task_prefix/venv/lib/python3.11/site-packages"

cmake --build "$task_prefix/build/opencv-build" --parallel "$task_build_jobs"
cmake --install "$task_prefix/build/opencv-build"
"$task_python" -c 'import cv2,numpy; print("OpenCV",cv2.__version__,"NumPy",numpy.__version__); print(cv2.getBuildInformation())'
