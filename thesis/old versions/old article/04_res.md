IV.	RESULT ANALYSIS
This section walks through the output produced at each stage of the pipeline, using data from the current run, followed by the results of the labelling algorithm evaluation described in Section III-G.
A.	Pose Landmark Extraction Output
The first output is from stage B of the methodology. pose_extractor.py was run across the UCF101 boxing clips, writing one row per frame to data/pose_frames.csv with 3D coordinates and a visibility score for 33 body landmarks. Table I shows example frames and columns for one landmark. The full CSV additionally includes the eyes, ears, mouth, shoulders, wrists, elbows, hips, knees, ankles, heels, and foot indices for the same frames. There is a total of 112 columns, and 31420 rows from 163 videos.
TABLE I. 	POSE_FRAMES.CSV EXAMPLE (SELECTED COLUMNS)
video_id	frame	timestamp_ms	pose_detected	left_shoulder_x	left_shoulder_y	left_shoulder_z	left_shoulder_vis
g01_c01	1	0	True	0.482
	0.353	0.111	0.999
g01_c01	12	440	True	0.448	0.378	-0.005
	0.999
g01_c01	18	680	True	0.490	0.334
	-0.011
	0.999
B.	Annotated Review Video Output
	This is the second output from stage B. pose_extractor.py also renders an annotated review video with the MediaPipe skeleton and the frame number overlaid on each frame (Fig. 2). This is the video a human reviewer watches to identify punch windows in the next stage.

Fig. 2.	Example annotated frame used for manual punch-window review.
C.	Manually Labelled Punch Windows
Reviewing the annotated videos from stage C of the methodology produced punch_windows.csv, which records the start and end frame of each observed punch. Table II shows an example entry.
TABLE II. 	PUNCH_WINDOWS.CSV EXAMPLE
video_id	start_frame	end_frame
g01_c01	15	21
g01_c01	22	26
g01_c01	27	31
D.	Training Dataset Construction Output
At stage D, build_training_csv.py converts each labelled interval, together with a matching set of sampled no_punch intervals, into a fixed-duration, millisecond-based window, and computes motion- and body-relative features from the surrounding pose frames rather than using raw landmark coordinates directly. Table III shows a subset of the computed features for three example windows (two punch windows from different videos and one no_punch window) to illustrate how the features differ between classes; the full row additionally includes elbow velocity and body-relative and shoulder-relative x/y/z statistics for both wrists and elbows.
TABLE III. 	TRAINING.CSV EXAMPLE (SELECTED COLUMNS)
video_id / window	right_wrist_velocity_mean	left_wrist_velocity_mean	right_shoulder_to_wrist_distance_change	right_wrist_forward_extension_change	label
g01_c01, 550–800 ms	3.634	1.991	0.134	0.059	punch
g02_c03, 5030–5280 ms	2.867	2.155	0.143	0.191	punch
g01_c03, 1000-1250 ms	0.791	1.126	0.029	-0.047	no_punch

	The larger right-wrist velocity and right-side shoulder-to-wrist distance change in the first punch example are consistent with a right-hand punch, illustrating why body-relative motion features, rather than raw landmark coordinates, are useful for distinguishing punch from no_punch windows.
	Table IV summarises the resulting training dataset composition.
TABLE IV. 	TRAINING DATASET COMPOSITION
Metric	Value
Punch windows	175
No_punch windows	175
Total training rows	350
Window duration	250 ms
Positive anchor mode	End
No_punch sampling stride	250 ms
Raw landmark mean/std features	Excluded
E.	Trained Classifier Model
At stage E, train_random_forest.py trains a Random Forest classifier on the 350-row training.csv dataset described in Section IV-D to distinguish punch from no_punch windows. Alongside the trained model (models/random_forest.joblib), the script saves the exact list of feature columns used during training, ensuring that later inference uses an identical feature structure.
F.	Predictions from New Footage
Then finally, predict_punches.py from stage F applies the trained classifier to a new, previously unseen user video. extract_user_pose_frames.py first produces user_pose_frames.csv in the same format as the training pose data; predict_punches.py then slides the same 250 ms window across this data, computes the identical set of features, and classifies each window as punch or no_punch. Per-window predictions are written to data/user_window_predictions.csv, and merged punch windows are written to data/predicted_punch_windows.csv, from which a final estimated punch count is derived.
TABLE V. 	USER_WINDOW_PREDICTIONS.CSV EXAMPLE (SELECTED COLUMNS)
video_id / window	right_wrist_velocity_mean	left_wrist_velocity_mean	right_shoulder_to_wrist_distance_change	right_wrist_forward_extension_change
test, 0–250 ms	3.141	2.054	-0.136	-0.136
TABLE VI. 	PUNCH_WINDOWS.CSV EXAMPLE
video_id	start_ms	end_ms
testvideo	280	570

G.	Evaluation of the Labelling Algorithm
Table VII reports the dataset-level results of the evaluation described in Section III-G, and Table VI maps each individual evaluation criterion to its result.
TABLE VII. 	LABELLING EVALUATION RESULTS
Metric	Result
Total videos with pose extracted	163
Pose Detection Coverage	95.4% (29,990 / 31,420 frames)
Videos manually reviewed	49
Videos usable and labelled	15 (31%)
Total punch windows labelled	175
Labels with missing pose video	0
Punch duration — mean	210.7 ms
Punch duration — std. dev.	56.1 ms
Punch duration — median	240 ms
Punch duration — min / max	80 / 400 ms
Audit status	Passed
Of the 49 reviewed videos, 15 (31%) yielded usable, labellable footage. The remaining 34 videos were excluded during review because of low frame rate, subjects not fully within frame, multiple people appearing in the shot, or partial obstruction of the boxer; in these cases MediaPipe either failed to track the boxer or produced erratic landmark estimates, and the affected videos were deliberately withheld from labelling rather than risk feeding unreliable pose data into the Random Forest model. The resulting 175 punch windows show a consistent duration distribution (mean 210.7 ms, median 240 ms), supporting the appropriateness of the 250 ms fixed window size used in Section III-D. No punch label referenced a missing pose video, and the audit reported an overall passed status. Pose detection succeeded for 95.4% of the 31,420 extracted frames (Table VII); the remaining frames most likely correspond to brief occlusion or fast motion blur within otherwise usable videos.
TABLE VIII. 	EVALUATION RESULTS BY CRITERION
Criterion	Result
Pose Detection Coverage	95.4% of frames had a successfully detected pose (29,990 / 31,420); see Table VII.
Label Completeness	5 of 15 labelled videos spot-checked against their annotated review video; no omitted punches found
Frame Boundary Accuracy	15 punch windows independently re-reviewed frame-by-frame
Label Integrity	0 missing video IDs; 0 invalid frame ranges (audit_punch_labels.py)
Frame Range Validation	0 out-of-range labels
Punch Window Validation	3 of 175 windows flagged as unusually short (80 ms); manually re-checked and start frame adjusted
Duplicate Label Detection	0 duplicate windows detected

H.	Result Discussion
The exclusion of 34 of the 49 reviewed videos illustrates a practical limit of relying on an uncurated action-recognition dataset such as UCF101: clips were not filmed for pose-estimation research, so a substantial share become unusable once MediaPipe's tracking requirements, which is a single, unobstructed, fully framed subject at a reasonable frame rate, are applied. This variability, rather than any weakness in the labelling algorithm itself, accounts for most of the gap between the 163 videos with extracted pose data and the 15 that contributed to the final training set.
