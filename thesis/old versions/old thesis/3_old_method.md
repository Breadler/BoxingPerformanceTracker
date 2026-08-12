CHAPTER 3
METHODOLOGY
3.1	Introduction
This chapter describes the methodology used to design, develop and evaluate an Android-based boxing performance tracker. The project prioritizes a practical balance where models and processing must be sufficiently accurate to be useful while remaining lightweight enough to run on smartphones. To achieve this, the study adopts an iterative prototyping approach that integrates camera-based pose estimation, feature engineering for boxing-specific metrics, an interpretable Random Forest classifier, and a 3D visualization pipeline for in-app playback and graphing.
The methodology is organized as a sequence of interlinked stages: 
i)	metric and problem definition,
ii)	dataset collection and standardization,
iii)	landmark extraction and feature engineering,
iv)	model development and mobile conversion,
v)	3D visualization and app integration, and
vi)	evaluation and testing.

Each stage includes iteration loops to allow refinement of features, thresholds, model parameters and user interface elements based on empirical results and user feedback. The next sections formalize the research design and the proposed methodology in detail.
 
3.2	Research Design
This research follows a quantitative experimental design combined with software engineering practices. The experimental component develops and evaluates a supervised learning classifier for frame-level boxing states (guard status, punch event, movement state) using engineered features derived from MediaPipe pose landmarks. The engineering component implements an Android application that integrates pose extraction, model inference, a synchronized 3D skeleton viewer, and time-series visualizations as shown in Figure 3.1.
The first key decision concerns the pose estimator. MediaPipe BlazePose was selected because its CNN-based architecture, built on a MobileNetV2 backbone, is optimized for mobile inference. It extracts 33 three-dimensional landmarks per frame, providing the necessary x,y,z coordinates for feature engineering and 3D visualization.
The second decision relates to the modeling strategy. This research uses an interpretable and lightweight Random Forest classifier trained on engineered features such as guard height, punch motion vectors, and movement variance. This approach ensures robust classification on limited, heterogeneous datasets while simplifying conversion to TensorFlow Lite for mobile deployment.
The final decision addresses the evaluation emphasis. Accuracy and explainability are prioritized over real-time speed, with testing conducted on held-out, phone-recorded validation data to reflect realistic deployment conditions.
             
Figure 3.1	Methodology Flowchart

3.3	Proposed Methodology
This section describes the steps used in the core pipeline. The proposed methodology is organized into five principal parts: data collection, pose-estimation pipeline, feature engineering, Random Forest model development, and 3D & graph visualization.

3.3.1	Data Collection
The data used in this study combines publicly available boxing video datasets with self-recorded videos. UCF101 (boxing subset) provides general movement patterns, while the Olympic Boxing Dataset offers labeled punch types for improved classification. Additionally, self-recorded videos, such as in Figure 3.2, simulate real-world conditions such as varied lighting and camera angles. All videos are processed using the same pipeline to extract 33 3D landmarks per frame via MediaPipe BlazePose, ensuring consistency across sources. Table 3.1 lists the datasets used.
Table 3.1	Data Collection
Dataset	Description	Purpose	Labeling
UCF101 (Boxing Subset)	Public video dataset containing general boxing movements.	Baseline training for overall boxing patterns.	Manual labelling
Olympic Boxing Punch Dataset (Kaggle)	Public dataset with labeled punch types.	Improves punch detection and classification.	Labeled
Self-Recorded Smartphone Videos	1–3 minute sessions recorded by researcher.	Final validation under real-world conditions, or more specific detection training.	Manual labelling

    
Figure 3.2	Self-Recorded Video Example

3.3.2	MediaPipe Landmark Extraction
After collecting videos from public datasets and self-recorded sessions, the next step is to extract pose landmarks for analysis. Each video is processed using MediaPipe BlazePose, which detects 33 3D landmarks per frame (x, y, z), as shown in Figure 3.3. Coordinates are normalized so that x and y correspond to image width and height, while z follows MediaPipe’s relative depth convention. All frames are exported at a fixed rate (30 FPS) to maintain temporal alignment across sessions. The output is stored in a structured CSV format for subsequent labeling and feature engineering. Table 3.2 describes the steps for landmark extraction.
 
Table 3.2	Landmark Extraction Workflow
Step	Description	Input	Tools	Output
1. Video Import	Load video from datasets or recordings for processing.	Collected datasets.	Python	Raw video frames
2. Pose Estimation	Apply BlazePose to extract 33 3D landmarks per frame with timestamps and source labels.	Video frames	MediaPipe BlazePose	CSV: frame, timestamp, x1..x33, y1..y33, z1..z33, source_label
3. Normalization	Normalize x and y to image dimensions; z uses relative depth convention.	Landmark coordinates	Python	Normalized landmark data
4. Export	Save processed data at 30 FPS for consistency across sessions.	Normalized landmarks	Python	Structured CSV files

    
Figure 3.3	Example of Body Landmarks from MediaPipe
3.3.3	Feature Engineering for Boxing Metrics
Feature engineering in this study focuses on transforming raw pose landmarks into meaningful metrics that reflect key aspects of boxing performance. Three primary movement categories are considered: guard position, punch execution, and footwork activity. 
•	Guard position is assessed by comparing wrist height to facial landmarks, enabling detection of guard drop over time. 
•	Punch execution is identified through forward wrist displacement combined with rotational movement of shoulders and hips, which signals striking actions. 
•	Footwork activity is measured by analyzing displacement variance in lower-body joints, providing insight into movement frequency and potential fatigue.
 These features are computed per frame or over short temporal windows and labeled according to boxing-specific states such as guard up/down, punch/no punch, and movement/non-movement. Table 3.3 summarizes these features.
Table 3.3	Summary of Core Features
Feature	Movement Focus	Measurement Approach	Labeling
Guard Height	Guard position (hands vs head)	Wrist height relative to facial landmarks	Guard up / Guard down
Punch Motion	Punch execution and rotation	Forward wrist movement + shoulder/hip rotation	Punch / No punch
Movement Variance	Footwork and repositioning frequency	Displacement variance of hips, knees, ankles	Movement / Non-movement

3.3.4	Random Forest Model Development
The objective of this stage is to train a classifier that maps engineered features to three frame-level states: guard up/down, punch/no punch, and movement/idle. A Random Forest model is chosen because it performs well on small-to-moderate datasets, handles noisy inputs effectively, and is resilient to overfitting. 
Additionally, Random Forests are straightforward to convert into TensorFlow Lite using TensorFlow Decision Forests. The development pipeline includes data partitioning, model training, evaluation, hyperparameter tuning, and conversion for Android integration. Table 3.4 outlines this pipeline, while Figure 3.4 provides an example of a decision tree from the Random Forest classifier.
Table 3.4	Random Forest Classifier Development Pipeline
Step	Task Description	Tools / Libraries	Expected Output
1. Data Split	Partition dataset into 80% training and 20% testing using stratified sampling.	Scikit-learn (train_test_split)	Stratified train/test datasets
2. Model Training	Train Random Forest on engineered features. Multi-output or separate models may be used for the three tasks.	Scikit-learn (RandomForestClassifier)	Trained .pkl Random Forest model
3. Evaluation	Compute Accuracy, Precision, Recall, F1-score, and confusion matrices for each task. Analyze detection latency and false positives.	Scikit-learn Metrics (classification_report, confusion_matrix)	Evaluation report with per-class metrics
Step	Task Description	Tools / Libraries	Expected Output
4. Hyperparameter Tuning	Optimize parameters using grid search and 5-fold cross-validation.	Scikit-learn (GridSearchCV)	Optimized RF model with improved metrics
5. Conversion to TensorFlow Lite	Convert the optimized Random Forest model into TensorFlow Lite format for mobile deployment.	TensorFlow Decision Forests	.tflite model for Android inference
6. Android Integration	Load .tflite model and run inference on pose-derived features inside the mobile app. Feed results into graphs and 3D visualization components.	Kotlin and TensorFlow Lite Interpreter	Fully integrated classification module

    
Figure 3.4	Example of Random Forest Decision Tree

3.3.5	3D and Graph Visualization
The objective of this stage is to render extracted pose coordinates as a rigged 3D skeleton and synchronise timeline playback with time-series graphs inside an Android application.
3D asset & rigging:
•	Author 1:1 skeleton in Autodesk Maya, matching MediaPipe landmark indices. Export an optimized .glb/glTF file suitable for Android rendering.
•	Ensure topology and joint pivots allow bone transforms driven directly by landmark coordinates.
Rendering & integration:
•	Use SceneView (Filament) to load .glb and render the skeleton. 
•	Implement a coordinate mapping layer in Kotlin that parses CSV or live MediaPipe output and applies joint transforms per frame (x,y,z → translation/rotation as appropriate). Apply interpolation (LERP) to smooth frame transitions and reduce jitter.
Timeline & graphs:
•	Implement a timeline controller to scrub, play, pause and jump to events. Use MPAndroidChart to plot GuardHeight, PunchMotion, and MovementVar as synchronized time-series.


3.4	System Design
This section describes the system-level design for the proposed app. The design is divided into a logical architecture (modules and data flow), interface layout (major screens & interactions), and data schema for pose and feature storage.

3.4.1	System Architecture (Logical Modules)
The system follows a modular, on-device-first architecture. Major modules:
•	Capture Module: handles video capture (CameraX) or video import. Produces MP4 or direct image frames.
•	Pose Extraction Module: runs MediaPipe BlazePose (Android SDK). Outputs 33 landmark coordinates per frame (x,y,z).
•	Feature Engine: computes engineered features (GuardHeight, PunchMotion, MovementVar).
•	RF Classification Module: runs the converted RF model (.tflite) to output frame-level classifications (guard/punch/movement) and per-frame probabilities.
•	Visualization Module: SceneView renders the 3D skeleton (.glb); MPAndroidChart renders synchronized time-series graphs and event markers; a timeline controller synchronizes playback and scrubbing.
•	UI / Interaction Module: screens for capture/import, analysis, 3D replay, graph dashboard, and session comparison.

Figure 3.5 outlines the logical architecture of the mobile application

    
Figure 3.5	System Architecture Flow


3.4.2	Data Schema
All stored sessions use a consistent CSV schema for landmarks and features:
•	Landmarks CSV: 
o	session_id, frame_idx, timestamp_ms, x1,...,x33, y1,...,y33, z1,...,z33
•	Features CSV: 
o	session_id, frame_idx, timestamp_ms, GuardHeight, PunchMotion, MovementVar, punch_prob, guard_prob, movement_prob

 
3.4.3	Interface & UX Flow
Primary screens: 
i)	Home / New Session: start camera or import video.
ii)	Session Setup: optional camera calibration step (place a marker, set distance).
iii)	Analysis Screen: SceneView 3D skeleton on top; timeline with play/pause/scrub in the middle, and graphs (GuardHeight, Punch Count, MovementVar) at bottom.

Figure 3.6 displays the proposed user interface for 3D skeleton playback and performance graphs.

 
Figure 3.6	App UI Design Mockup

3.5	Implementation
This section describes the development environment, core modules, model conversion steps, 3D asset pipeline, and synchronization logic required to deploy the system on Android.

3.5.1	Development Environment & Tools
Table 3.5	Development Environments
Component	Tool / Framework
Mobile App Development	Android Studio (Kotlin), CameraX, MPAndroidChart
Pose Estimation	MediaPipe BlazePose (Android SDK)
3D Rendering	SceneView (Filament) with glTF (.glb) assets
Random Forest	Python (mediapipe, numpy, pandas, scikit-learn)
Model Conversion	TensorFlow Decision Forests to TFLite
3D Asset Creation	Autodesk Maya to glTF export


3.5.2	Module Implementation Details
Pose Extraction:
•	Use Python and MediaPipe to process datasets and self-recorded videos. Produce landmarks CSVs.
•	Apply preprocessing smoothing: 3-frame moving average for coordinates and a frame-level quality filter (discard frames where detection confidence < 0.5).
Feature Engine:
•	Implement feature functions in Python for batch processing and in Kotlin for in-app processing. Keep identical logic in both languages to ensure parity (copy-tested by unit tests).
•	Temporal features computed with fixed windows (such as 1-second sliding window for MovementVar).
Model Training & Conversion:
•	Train RandomForestClassifier in scikit-learn on the engineered features.
•	Save a .pkl for offline evaluation.
•	Convert the RF to a TensorFlow Decision Forest or re-implement the RF logic in TensorFlow for TFLite conversion. Produce a .tflite file for Android inference.
•	Validate .tflite predictions against the .pkl outputs for consistency.
Android Inference Integration:
•	Load .tflite with TensorFlow Lite Interpreter in Kotlin.
•	Use MediaPipe, feed per-frame features directly to the TFLite model for immediate classification.
•	Store per-frame inferences for visualization.
3D Skeleton & Mapping:
•	Rig MGM skeleton in Maya with joint names matching MediaPipe indices.
•	Export .glb and ensure joint orientation/pivot alignment.
•	In Kotlin, implement a PoseMapper that converts normalized MediaPipe coordinates into SceneView coordinate space (accounting for camera aspect ratio, orientation, and optional scaling factor). Apply per-joint transforms to 3D skeleton.
•	Implement interpolation (LERP) for positions and SLERP if rotations used, using a smoothing factor adjustable in settings.
Timeline & Graph Synchronization:
•	Build a TimelineController that advances frames and notifies both SceneView (apply joint transforms) and MPAndroidChart (append or display current time index). 

3.5.3	Validation & Unit Tests
The testing strategy includes multiple levels to ensure system reliability. Unit tests validate feature functions such as guard height, punch motion, and movement variance in Python. Integration tests confirm that a known landmark CSV produces the expected feature matrix and classification output. End-to-end tests on Android verify that SceneView animations match the expected coordinates using recorded test CSVs. Finally, device-level testing with self-recorded videos assesses real-world performance and consistency.

 
3.6	Experimental Setup
This section formalizes the environment, datasets, evaluation protocol, and testing conditions used to measure system performance. The emphasis is on reproducibility and on producing metrics that reflect mobile deployment scenarios.

3.6.1	Data Sources and Recording Protocol
The evaluation uses two primary sources of data: public datasets and self-recorded smartphone videos. Public datasets provide standardized benchmarks for assessing the system’s ability to recognize boxing movements and detect punches. Specifically, the UCF101 boxing subset offers general movement patterns, while the Olympic Boxing Punch dataset (Kaggle) includes labeled punch events, enabling a more detailed evaluation of detection accuracy.
To complement these controlled datasets, self-recorded videos are used to validate the system under realistic mobile conditions. These videos consist of 3–6 short sessions, typically 2–3 minutes each, captured using different camera angles. These recordings are reserved exclusively for testing and validation, preventing any data leakage from the training phase.
All self-recorded sessions follow a consistent protocol to maintain reproducibility:
•	Device: mid-range Android smartphone (Snapdragon 700 series or equivalent). Record device model per session.
•	Resolution / Frame rate: 1080p @ 30 FPS.
•	Camera placement: 2.5 – 3.5 meters from subject; camera height at mid-chest level; frontal or slight 15–30° angle.
•	Lighting: indoor gym lighting.
•	Session content: structured drills such as jab-cross combinations (30–60s), hook combinations (30–60s), movement and combination work (30–60s), rest intervals.

3.6.2	Evaluation Metrics & Procedures
Because the system emphasizes accuracy and explainability rather than low-latency throughput, the evaluation protocol focuses on the following metrics:
•	Pose Estimation Quality: RMSE per joint (mm) and average joint error. 
•	Guard Detection: Accuracy (%) of guard_up/down classification.
•	Punch Detection: Precision, Recall, F1-score for punch events (straights, hooks where labeled). 
•	Movement Frequency: Correlation (R²) between movement variance and annotated activity segments.
•	Visualization Clarity: User-rated Likert-scale metric (1–5) on ease of interpretation of 3D replay and graphs.

Evaluation Steps: 
i)	Run offline pose extraction on test datasets; compute feature matrix X test.
ii)	Run RF .tflite inference per frame and aggregate predictions to compute metrics.
iii)	For punch detection, allow 200 ms tolerance to count a detection as correct (to account for annotation variance).
iv)	Evaluate visualization alignment by overlaying skeleton on original video and computing a visual alignment score (qualitative and spot-check quantitative per-frame offsets).

3.6.3	Testing Conditions & Controls
Because the system prioritizes accuracy and interpretability over low-latency performance, the testing conditions are designed to capture a wide range of real-world variability. To achieve this, the evaluation includes scenarios with different camera angles, variations in clothing such as loose versus tight attire, and environments with varying levels of background clutter. These factors help assess the robustness of the model under conditions that users are likely to encounter.
Additionally, ambient conditions are recorded as metadata for every test session. This includes lighting levels, the specific camera model used, and the distance between the subject and the camera. Capturing these details allows for correlation analysis between environmental factors and any observed performance degradation, providing insights into the conditions that most affect system accuracy.

 
3.7	Evaluation Strategy
The evaluation strategy focuses on determining whether the proposed system can reliably extract 3D pose data from mobile footage, classify key boxing movements with sufficient accuracy, and present visual outputs in a way that meaningfully supports performance analysis. Since the system does not measure fatigue directly, evaluation also verifies whether temporal trends, such as guard decline or reduced punching frequency, are displayed clearly enough for users to interpret fatigue themselves.
The assessment is conducted across three dimensions: Model Accuracy Evaluation, Pose Quality and 3D Visualization Validation, and User-Centered Evaluation of Interface and Clarity. Table 3.6 defines the metrics and target values.
Table 3.6	Evaluation Matrix
Aspect	Metric	Description	Target Value	Tools
Pose Estimation Accuracy	RMSE (mm)	Compare extracted coordinates vs ground truth dataset	< 10 mm	Python
Guard Detection	Accuracy (%)	Correct classification of guard up/down	> 90%	Scikit-learn
Punch Detection	F1-Score	Identifies straights, hooks, uppercuts	> 85%	Scikit-learn
Movement Frequency	R² Correlation	Model vs annotated movement activity	> 0.8	Python
Visualization Clarity	User rating (1–5)	Ease of interpreting replay & trends	> 4.0	User study


3.7.1	Model Accuracy Evaluation
The Random Forest classifier is evaluated on a test partition of the engineered feature dataset. Accuracy, Precision, Recall, and F1-score are used to measure performance for the three boxing tasks: guard detection, punch detection, and movement classification. Confusion matrices are generated to identify misclassification patterns and guide hyperparameter tuning. A target accuracy above 85–90% is expected for each task based on comparable studies.

3.7.2	Pose Quality and 3D Visualization Validation
Since MediaPipe is the foundation of all downstream components, extracted 3D coordinates are validated against reference datasets containing ground-truth motion capture. Root Mean Square Error (RMSE) is used as the primary comparison metric. The 3D viewer is tested for skeletal alignment, motion smoothness, temporal synchronization with the video timeline, and coordinate mapping integrity. Test sessions are recorded under different lighting and camera distances to reflect real user conditions.

 
3.7.3	User-Centered Evaluation of Interface and Clarity
A small user evaluation is conducted with athletes, boxers, and non-athletes. Participants review a recorded session using the app’s playback screen. They rate clarity of motion, ease of graph interpretation, and usefulness of trend displays on a 1–5 scale. This ensures that the visualization and interface serve practical user needs rather than only technical correctness.
Together, these steps verify both technical accuracy and functional usefulness, ensuring that the system operates as a reliable training-analysis tool rather than just a pose-estimation demo.

 
3.8	Summary
This chapter outlined the complete methodological framework for developing a mobile-based AI system that analyzes boxing performance using camera-only input. The approach integrates three core components: MediaPipe BlazePose for 3D pose estimation, feature engineering for boxing-specific metrics, and a Random Forest classifier for detecting guard position, punch events, and movement frequency. These elements are combined into an Android application that also includes a 3D skeleton replay and synchronized time-series graphs for intuitive visualization.
The methodology begins with data acquisition, leveraging public boxing datasets and self-recorded videos to ensure both controlled and real-world testing conditions. Pose landmarks are extracted and normalized, then transformed into engineered features such as guard height ratios, punch motion vectors, and movement variance. These features feed into a lightweight Random Forest model optimized for mobile deployment via TensorFlow Lite.
System design emphasizes modularity, covering video capture, pose extraction, feature computation, classification, and visualization. A rigged 3D skeleton is implemented for playback, synchronized with graphical dashboards to provide dual-layer analysis. The evaluation strategy prioritizes accuracy and interpretability over real-time speed, with metrics including pose RMSE, classification accuracy, F1-score, and visualization clarity.
With this methodology defined, Chapter 4 proceeds to present the results of model performance, the behaviour of the visualization system, and the practical insights gained from testing and validation.
