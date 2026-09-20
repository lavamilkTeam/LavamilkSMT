# STM32F407: board clock, pinout, startup code and linker script belong to the board project.
set(CMAKE_SYSTEM_NAME Generic)
set(CMAKE_SYSTEM_PROCESSOR arm)
set(CMAKE_TRY_COMPILE_TARGET_TYPE STATIC_LIBRARY)

find_program(CMAKE_C_COMPILER arm-none-eabi-gcc REQUIRED)
find_program(CMAKE_CXX_COMPILER arm-none-eabi-g++ REQUIRED)
find_program(CMAKE_ASM_COMPILER arm-none-eabi-gcc REQUIRED)
find_program(CMAKE_OBJCOPY arm-none-eabi-objcopy REQUIRED)
find_program(CMAKE_SIZE arm-none-eabi-size REQUIRED)

set(STM32_CPU_FLAGS "-mcpu=cortex-m4 -mthumb -mfpu=fpv4-sp-d16 -mfloat-abi=hard")
set(CMAKE_C_FLAGS_INIT "${STM32_CPU_FLAGS} -ffunction-sections -fdata-sections")
set(CMAKE_CXX_FLAGS_INIT "${STM32_CPU_FLAGS} -ffunction-sections -fdata-sections")
set(CMAKE_ASM_FLAGS_INIT "${STM32_CPU_FLAGS} -x assembler-with-cpp")
set(CMAKE_EXE_LINKER_FLAGS_INIT "${STM32_CPU_FLAGS} -Wl,--gc-sections")
set(CMAKE_EXECUTABLE_SUFFIX ".elf")
