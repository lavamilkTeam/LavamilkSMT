import cv2


class PreviewProcessor:
    """只处理传入图像，供正式算法替换；结果不作为元件/Mark 定位结论。"""

    def process(self, frame) -> dict:
        gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
        blurred = cv2.GaussianBlur(gray, (5, 5), 0)
        _, mask = cv2.threshold(blurred, 0, 255, cv2.THRESH_BINARY + cv2.THRESH_OTSU)
        contours, _ = cv2.findContours(mask, cv2.RETR_LIST, cv2.CHAIN_APPROX_SIMPLE)
        max_area = frame.shape[0] * frame.shape[1] * 0.95
        candidates = [c for c in contours if 30 <= cv2.contourArea(c) <= max_area]
        candidates = sorted(candidates, key=cv2.contourArea, reverse=True)[:100]
        annotated = cv2.cvtColor(gray, cv2.COLOR_GRAY2BGR)
        cv2.drawContours(annotated, candidates, -1, (0, 255, 0), 1)
        cv2.putText(annotated, f"Contours: {len(candidates)} (preview)", (10, 24),
                    cv2.FONT_HERSHEY_SIMPLEX, 0.55, (0, 255, 0), 1, cv2.LINE_AA)
        return {"gray": gray, "threshold": mask, "contours": annotated}
