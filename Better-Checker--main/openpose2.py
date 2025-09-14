import sys, json, time, argparse
import cv2 as cv
import mediapipe as mp

# ---------- CLI ----------
ap = argparse.ArgumentParser()
ap.add_argument("--src", default="1", help="camera index (e.g., 0) or video path")
ap.add_argument("--w", type=int, default=640)
ap.add_argument("--h", type=int, default=360)
ap.add_argument("--show", action="store_true", help="show annotated video window")
ap.add_argument("--face", action="store_true", help="include face landmarks in JSON")
args = ap.parse_args()

# ---------- Config ----------
FRAME_SIZE = (args.w, args.h)
INCLUDE_FACE = args.face
SHOW = args.show

# parse src
src = 1
if args.src.isdigit():
    src = int(args.src)
else:
    src = args.src  # treat as path

# ---------- MediaPipe ----------
mp_holistic = mp.solutions.holistic
mp_pose = mp.solutions.pose
mp_hands = mp.solutions.hands
mp_draw = mp.solutions.drawing_utils
mp_style = mp.solutions.drawing_styles

pose_names  = [e.name for e in mp_pose.PoseLandmark]
hand_names  = [e.name for e in mp_hands.HandLandmark]

def lm_to_list(landmarks, names=None, include_visibility=False):
    out = []
    for i, lm in enumerate(landmarks.landmark):
        item = {"i": i, "x": lm.x, "y": lm.y, "z": lm.z}
        if include_visibility and hasattr(lm, "visibility"):
            item["v"] = lm.visibility
        if names is not None and i < len(names):
            item["name"] = names[i]
        out.append(item)
    return out

def face_to_list(landmarks):
    out = []
    for i, lm in enumerate(landmarks.landmark):
        out.append({"i": i, "x": lm.x, "y": lm.y, "z": lm.z})
    return out

# ---------- Video ----------
cap = cv.VideoCapture(src)
# (optional) try to help Windows’ camera backend
cap.set(cv.CAP_PROP_FRAME_WIDTH, FRAME_SIZE[0])
cap.set(cv.CAP_PROP_FRAME_HEIGHT, FRAME_SIZE[1])

if not cap.isOpened():
    print(json.dumps({"type":"error","msg":"cannot_open_input","src":str(src)}), flush=True)
    sys.exit(1)

if SHOW:
    cv.namedWindow("Holistic", cv.WINDOW_NORMAL)
    cv.resizeWindow("Holistic", FRAME_SIZE[0], FRAME_SIZE[1])

with mp_holistic.Holistic(
        static_image_mode=False,
        model_complexity=1,
        refine_face_landmarks=False,
        min_detection_confidence=0.5,
        min_tracking_confidence=0.5
) as holo:
    try:
        while True:
            ok, frame = cap.read()
            if not ok:
                break

            if FRAME_SIZE:
                frame = cv.resize(frame, FRAME_SIZE)
            rgb = cv.cvtColor(frame, cv.COLOR_BGR2RGB)

            ts = time.time()
            res = holo.process(rgb)

            # ---- Build JSON payload ----
            msg = {
                "type": "holistic",
                "ts": ts,
                "size": {"w": frame.shape[1], "h": frame.shape[0]}
            }
            if res.pose_landmarks:
                msg["pose"] = lm_to_list(res.pose_landmarks, names=pose_names, include_visibility=True)
            if res.left_hand_landmarks:
                msg["left_hand"] = lm_to_list(res.left_hand_landmarks, names=hand_names)
            if res.right_hand_landmarks:
                msg["right_hand"] = lm_to_list(res.right_hand_landmarks, names=hand_names)
            if INCLUDE_FACE and res.face_landmarks:
                msg["face"] = face_to_list(res.face_landmarks)
            if len(msg) == 3:
                msg["note"] = "no_landmarks"

            print(json.dumps(msg), flush=True)

            # ---- Draw & show (optional) ----
            if SHOW:
                if res.pose_landmarks:
                    mp_draw.draw_landmarks(
                        frame, res.pose_landmarks,
                        mp_pose.POSE_CONNECTIONS,
                        landmark_drawing_spec=mp_style.get_default_pose_landmarks_style()
                    )
                if res.left_hand_landmarks:
                    mp_draw.draw_landmarks(
                        frame, res.left_hand_landmarks,
                        mp_hands.HAND_CONNECTIONS,
                        mp_draw.DrawingSpec(thickness=1, circle_radius=2),
                        mp_draw.DrawingSpec(thickness=1)
                    )
                if res.right_hand_landmarks:
                    mp_draw.draw_landmarks(
                        frame, res.right_hand_landmarks,
                        mp_hands.HAND_CONNECTIONS,
                        mp_draw.DrawingSpec(thickness=1, circle_radius=2),
                        mp_draw.DrawingSpec(thickness=1)
                    )
                # (Face connections are heavy; enable if needed.)
                cv.imshow("Holistic", frame)
                if cv.waitKey(1) & 0xFF == ord('q'):
                    break
    except KeyboardInterrupt:
        pass

cap.release()
if SHOW:
    cv.destroyAllWindows()