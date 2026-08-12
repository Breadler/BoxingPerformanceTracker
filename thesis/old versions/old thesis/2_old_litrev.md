CHAPTER 2
LITERATURE REVIEW
2.1	Introduction
Recent advancements in Artificial Intelligence (AI), particularly convolutional neural networks (CNNs), have enabled markerless pose estimation directly from standard RGB video. This unlocks a more accessible way to track movement, technique, and physical performance without specialised hardware.
Within striking sports such as boxing, pose estimation research is growing but still relatively limited. Much of the existing work focuses on isolated aspects of performance, such as punch count for judging matches, rather than providing a time-based analysis of technical degradation or fatigue across rounds. This chapter reviews prior studies that form the foundation for this research project. Each study is examined with respect to its purpose, methods, strengths, weaknesses, and relevance to the proposed boxing-tracking mobile application.

 
2.2	Pose Estimation Studies
2.2.1	AI in Sports Biomechanics (Souaifi et al., 2025) 
In the study, Artificial Intelligence in Sports Biomechanics: A Scoping Review on Wearable Technology, Motion Analysis, and Injury Prevention, Souaifi et al. (2025) conducted a broad scoping review examining the application of AI across sports biomechanics, including wearable sensors, markerless vision systems, and machine learning-based analytical models. Their review highlights motion capture technologies as well as macro-level trends, such as the shift from controlled laboratory environments toward mobile, real-time solutions. This validates the overall direction of this project, which aims to bring pose analysis to smartphones.
A strength of their review is its extensive coverage, which allows it to identify recurring issues: high cost of hardware-based systems, limited portability of lab-grade setups, and the emerging preference for markerless AI pipelines. However, the paper does not provide detailed implementation guidance or evaluate any specific approach experimentally. For this project, this study confirms that affordable, accessible, AI-driven motion analysis is a recognised need, and that markerless CNN-based methods are increasingly feasible on consumer devices. Table 2.1 summarizes this study.
 
Table 2.1	Artificial Intelligence in Sports Biomechanics
Author	Year	Purpose	Method/Tech Used	Strengths	Weaknesses
Souaifi et al.	2025	Examines AI applications in sports biomechanics such as wearables, motion capture, and analytics.	Systematic scoping review of literature that summarizes methods, including wearables, markerless vision, and ML models for classification or prediction.	- Wide literature coverage
- Identifies trends and gaps from lab to on-device solutions	- Broad scope 
- Lacks deep experimental details for any single method.


2.2.2	Pose Estimation for Medical and Exercise Assessment (Patil et al., 2022) 
In the study, Body Posture Detection and Motion Tracking using AI for Medical Exercises and Recommendation System, Patil et al. (2022) presented a pose-based exercise assessment system that uses a webcam or smartphone camera to automatically analyse medical rehabilitation movements. Their work demonstrates how lightweight pose estimation can support structured feedback without wearables, which provides a practical foundation for sports-oriented applications. Their system successfully combined pose estimation, rule-based scoring, and a dashboard interface. This shows that a full end-to-end pipeline from camera input to performance feedback is achievable outside laboratory settings.
This study’s demonstration of a functional on-device pipeline aligns with this project’s requirement to run pose extraction and analytics on mobile hardware. However, their system is deliberately designed for slow, controlled movements such as physiotherapy exercises. Such motions are predictable and have low variability, meaning the pose estimator is rarely challenged by occlusion or high-speed dynamics. As a result, the approach cannot be directly applied to fast boxing movements, where rapid accelerations and torso rotation introduce greater landmark noise. Table 2.2 summarizes this study.
Table 2.2	Body Posture Detection and Motion Tracking using AI for Medical Exercises and Recommendation System
Author	Year	Purpose	Method/Tech Used	Strengths	Weaknesses
Patil et al.	2022	Demonstrated a camera-based posture detection system for medical or rehab exercises and automated feedback without wearables.	Development of a program with pose estimation, web dashboard, rule-based accuracy counting, and tested on common exercises.	- Practical mobile/web pipeline
- Showed high inference speed and good counting accuracy 	- Not focused on not sports fatigue over rounds
- Evaluation limited to small test sets and controlled conditions.


2.2.3	Real-Time Boxing Feedback System (Bulun & Berg, 2024)) 
In the study, Real-Time Boxing Feedback Using Human Pose Estimation and Machine Learning, Bulun and Berg (2024) developed an AI system targeted specifically at boxing. Their mobile prototype detects jab punches and determines whether the athlete’s guard drops afterward. They used MediaPipe BlazePose for pose extraction and a Random Forest classifier to detect punch frames, with the guard height computed using threshold-based logic relative to the shoulder position. This work demonstrates that pose-based analysis is already functioning at real-time speeds on mobile devices, validating the feasibility of smartphone-based boxing feedback.
The main strength of their study is its clear demonstration that pose estimation can support practical boxing feedback, achieving good accuracy even on handheld mobile video. The transition from a desktop prototype to an Android TensorFlow Lite deployment provides a useful engineering model. However, the system is intentionally narrow: it only identifies jabs and only detects a specific error (guard drop immediately after a jab). It does not measure performance over an entire session, track hooks or uppercuts, or analyse overall movement patterns. Their study provides a great basis to follow, and this project builds directly on their findings by expanding from a single-event detector to a multi-metric performance tracker with guard trend analysis, punch volume tracking, full-body movement frequency, and 3D visualization. Table 2.3 summarizes this study.
Table 2.3	Real-Time Boxing Feedback Using Human Pose Estimation and Machine Learning
Author	Year	Purpose	Method/Tech Used	Strengths	Weaknesses
Bulun & Berg	2024	Investigates a system to provide real-time feedback to boxers on guard position in mobile app form	Development of a program with pose estimation, ML punch detection model, threshold-based checks, and audio feedback	- Strong practical demonstration. 
- Good accuracy. 
- Applied to boxing specifically	- Focused only on guard drop. 
- Does not analyze full degradation over rounds. 
- Limited to one feedback type


2.2.4	Accuracy of Markerless Motion Capture for Boxing (Magalhães et al., 2022) 
In the study, Accuracy of a Markerless Motion Capture System in Estimating Upper Extremity Kinematics During Boxing, Magalhães et al. (2022) evaluated the accuracy of Theia3D, a high-quality markerless motion capture system, by comparing its results to a marker-based reference system during boxing tasks. Their experiments show that markerless vision systems can achieve strong kinematic accuracy, even for fast punch movements, when using multi-camera, high-frame-rate setups. This provides important evidence that boxing motion can be effectively captured using only video, which supports this project’s use of 3D pose estimation.
This study confirmats that vision-based methods are sufficiently accurate for boxing analysis, especially for upper-body mechanics. However, their setup uses professional-grade cameras, careful lighting, and a controlled environment, making it impractical for casual consumer use. This project therefore shifts the same concept into a more accessible context: relying on a smartphone camera and an embedded CNN pose estimator (MediaPipe BlazePose), accepting slightly lower accuracy in exchange for usability and low cost. Table 2.4 summarizes this study. 
Table 2.4	Accuracy of a Markerless Motion Capture System in Estimating Upper Extremity Kinematics During Boxing
Author/Title	Year	Purpose	Method/Tech Used	Strengths	Weaknesses
Magalhães et al.	2022	Evaluated a markerless motion capture for boxing kinematics, finding strong accuracy in tracking punches and upper body movement.	Experimental comparison between markerless and marker-based capture systems with trained boxers performing jab–cross combinations.	- Validates reliability of vision-based tracking in boxing
- Confirms feasibility of non-wearable motion analysis	- Requires high-quality cameras and controlled setup
- Not optimized for mobile or real-time use

2.2.5	2D-to-3D Pose Model Transfer for Boxing (Lin et al., 2023)
In the study, Model Transfer from 2D to 3D Study for Boxing Pose Estimation, Lin et al. (2023) explored improvements in boxing pose estimation by extending 2D pose models into 3D using transfer learning and RGB channel patching. Their work demonstrates that monocular RGB video contains enough implicit depth cues for meaningful 3D reconstruction, and that model transfer techniques can significantly improve accuracy compared to relying only on 2D video analysis. This is important because boxing involves occluded limbs, rapid torso rotation, and complex multi‑planar movements that 2D coordinates alone cannot capture reliably. By contrast, 3D coordinates provide richer spatial information, enabling more precise tracking of punches, guard position, and fatigue‑related form degradation.
This study shows that 3D boxing pose estimation is both feasible without depth sensors and preferable to 2D approaches. However, their models are computationally heavy and designed for offline experiments rather than mobile deployment. The architecture complexity and training cost make it unsuitable for on‑device inference. This project instead adopts MediaPipe BlazePose, which achieves efficient real‑time 3D landmark estimation suitable for use in Android apps. Table 2.5 summarizes this study.
 
Table 2.5	Model Transfer from 2D to 3D Study for Boxing Pose Estimation
Author/Title	Year	Purpose	Method/Tech Used	Strengths	Weaknesses
Lin et al.	2023	Developed a model transfer approach using RGB channel patching to adapt 2D pose estimation models for 3D boxing pose estimation. 	Implemented transfer learning on 2D models (OpenPose, Hourglass, HRNet) with RGB channel patching and keypoint accuracy analysis.	- Demonstrates method to improve 3D boxing pose estimation from regular cameras
- Quantitative accuracy gains up to 20%	- Focused on algorithmic benchmarking
- Complex for small-scale deployment


2.2.6	Summary of Literature Findings
The reviewed body of work collectively demonstrates that camera-based human pose estimation is a practical technique for analysing athletic movement, supported by advances in CNN-based landmark detection. Across studies in sports biomechanics, medical exercise assessment, and boxing-specific pose estimation, researchers have shown that modern markerless systems can extract meaningful joint kinematics with high accuracy, even during dynamic upper-body movements. This establishes a clear foundation for developing accessible, low-cost motion analysis tools without reliance on wearable sensors or laboratory-grade equipment.
However, the literature also reveals several important limitations that motivate the present project. First, most existing systems focus on single-event analysis, such as counting repetitions, rather than evaluating performance over an entire training session. None of the reviewed studies implement time-based analytics to monitor how technique changes as fatigue accumulates. Second, no existing solution integrates clear visualization within a mobile app to help non‑technical users easily interpret performance and fatigue trends in boxing. Finally, prior work tends to rely either on controlled laboratory environments or on simplified exercises, which do not generalize to high-intensity striking performance.
In summary, the literature shows that while pose estimation, machine learning-based classification, and mobile inference, are well established, there remains a clear gap in systems that combine these components into a comprehensive, athlete-oriented tool. Therefore, this project aims to address these gaps by building on previous work beyond single-event detection toward session-level trend analysis, providing a more practical approach to tracking technical degradation and training in boxing. Table 2.6 consolidates findings from pose estimation studies.
 
Table 2.6	Comparative Study of Pose Estimation Literature Review
Source	Main Findings	Methodology	Strengths	Weaknesses	Relevance to Project
Souaifi et al.  (2025)
Artificial Intelligence in Sports Biomechanics	Reviewed AI use in sports biomechanics including wearables, and motion capture.	Systematic review summarizing methods such as markerless vision and ML classification.	Broad coverage; identifies trend toward on-device tracking.	Lacks experimental validation.	Highlights need for affordable, mobile motion-tracking systems.
Patil et al. (2022)
Body Posture Detection and Motion Tracking using AI	Developed camera-based posture tracking and feedback for rehab exercises.	Pose estimation with web dashboard and rule-based accuracy checking.	Practical implementation; fast and accurate counting.	Focused on rehab, not sports or fatigue tracking.	Serves as a model for on-device pose tracking and performance feedback.
Bulun & Berg (2024) Real-Time Boxing Feedback Using Pose Estimation 	Created mobile app for real-time boxing feedback using pose estimation.	ML-based punch detection and guard tracking with threshold checks.	High accuracy; practical real-world use.	Limited to guard-drop detection; lacks multi-round analysis.	Provides reference for boxing-specific motion analysis and tracking.
Magalhães et al. (2022) Accuracy of a Markerless Motion Capture System 	Validated vision-based motion capture accuracy in boxing.	Compared markerless vs. marker-based tracking on jab–cross drills.	Reliable tracking; confirms feasibility without wearables.	Needs high-quality cameras; not real-time.	Confirms vision-based tracking can reach lab-level accuracy for boxing.
Lin et al. (2023) Model Transfer from 2D to 3D for Pose Estimation	Improved 3D pose estimation from 2D models via RGB channel patching.	Transfer learning on OpenPose, Hourglass, HRNet architectures.	Significant accuracy gains (~20%); strong algorithmic insight.	Computationally heavy; unsuitable for mobile use.	Supports CNN-based 3D pose estimation for striking sports.

2.3	Random Forest Studies
2.3.1	3D Human Pose Estimation Algorithm Using Single Depth Images for Pose Analysis (Park et al., 2017) 
In the study, Accurate and Efficient 3D Human Pose Estimation Algorithm Using Single Depth Images for Pose Analysis in Golf (Park et al., 2017), the authors propose a three-stage random forest pipeline that combines random regression forests with verification forests to improve precision and efficiency of 3D joint localization from depth images. Their method, evaluated on a large golf-swing dataset, achieved higher accuracy and lower computation time compared to baseline forest approaches, and its feasibility was demonstrated through a prototype swing-analysis system (SWAN). A key strength of this work is the verification stage, which reduces noisy vote contributions and improves accuracy for occluded joints while lowering computational cost.
This study shows that tree-based ensembles can enhance pose estimation accuracy and robustness, particularly for occluded or fast movements such as punches. However, the approach relies on depth camera inputs, making it unsuitable for standard RGB smartphone video. This project instead adopts MediaPipe BlazePose, which provides efficient real-time 3D landmark estimation from RGB input, enabling mobile deployment without specialized hardware. The ensemble principles and vote-verification concept from Park et al.’s work will be adapted to filter noisy frames and improve robustness before feature computation, ensuring reliable boxing-state classification on consumer devices. Table 2.7 summarizes this study.
 
Table 2.7	3D Human Pose Estimation Algorithm Using Single Depth Images for Pose Analysis
Author	Year	Purpose	Method/Tech Used	Strengths	Weaknesses
Park et al.	2017	Improve 3D pose estimation accuracy/efficiency for golf swing analysis	Random regression forests and verification forests on depth images.	- Robust to occlusion
- improved accuracy & compute vs baseline	- Depends on depth camera input


2.3.2	Random Forest Algorithm for Identifying Abnormal Patterns of Knee Joint Movement (Prodanović et al., 2024) 
In the study Application of the Random Forest Algorithm for Identifying Abnormal Patterns of Knee Joint Movement (Prodanović et al., 2024), the authors applied Random Forest classifiers to joint kinematic data to detect abnormal knee movement patterns. Their approach demonstrated strong classification performance on clinical motion-capture datasets and included detailed discussion of preprocessing, feature selection, and labeling strategies for joint-level abnormality detection. A key strength of this work is its clear evidence that Random Forests handle noisy joint coordinate data effectively and can discriminate subtle kinematic differences when features are engineered correctly.
However, the dataset used in this study was high-fidelity clinical motion-capture data, and the labeled anomalies were domain-specific, which limits direct transfer to smartphone video inputs. This project instead applies Random Forest classifiers to classify boxing guard states and movement patterns using engineered features derived from MediaPipe landmarks, such as guard height ratio, hip and shoulder rotational deltas, and movement variance. From Prodanović et al.’s approach, this project adopts best practices in preprocessing and feature selection, including creating robust, noise-resistant features, applying careful labeling and cross-validation, and performing domain adaptation or fine-tuning using smartphone recordings to bridge the gap between laboratory-grade and consumer-level data. Table 2.8 summarizes this study.
Table 2.8	Random Forest Algorithm for Identifying Abnormal Patterns of Knee Joint Movement
Author	Year	Purpose	Method/Tech Used	Strengths	Weaknesses
Prodanović et al.	2024	Detect abnormal knee joint movement patterns using ML	Random Forests on motion-capture joint kinematics, feature engineering and labeling	- Demonstrates RF robustness on joint kinematics
- clear preprocessing/labeling workflows	- Uses high-fidelity mocap data


2.3.3	Automatic Activity Classification and Movement Assessment During a Sports Training Session (Ahmadi et al., 2014) 
In the study Automatic Activity Classification and Movement Assessment During a Sports Training Session Using Wearable Inertial Sensors (Ahmadi et al., 2014), the authors developed an ambulatory motion analysis system using wearable IMUs. They extracted time-frequency features via Discrete Wavelet Transform (DWT) and classified training activities using a Random Forest model, achieving classification accuracy of up to approximately 98% for certain activity classes. The study also explored methods for estimating joint angles and comparing subjects to normative curves. A key strength of this work is the demonstrated high accuracy of a relatively lightweight pipeline (DWT combined with Random Forest) in unconstrained training environments, along with practical feature extraction techniques that are computationally efficient.
However, the input modality in this study was wearable IMUs, which means direct transfer to vision-based landmarks requires rethinking feature computation. This project instead adapts the concept by computing temporal features from pose landmarks, such as velocity, acceleration, and short-window FFT or DWT of joint trajectories, to capture punch bursts and movement patterns. These features will then be fed into a Random Forest classifier for punch and movement classification. The success of Ahmadi et al.’s lightweight pipeline with small datasets as shown in Table 2.9 using similar efficient approaches in this project, enabling accurate classification without relying on heavy deep learning models. Table 2.9 summarizes this study.
Table 2.9	Automatic Activity Classification and Movement Assessment During a Sports Training Session
Author	Year	Purpose	Method/Tech Used	Strengths	Weaknesses
Ahmadi et al.	2014	Activity classification during sports sessions with wearable IMUs	DWT time-frequency features and Random Forest classifier, and orientation estimation algorithms	- Very high classification accuracy
- works in unconstrained environments	- Relies on IMU sensors (not vision)
- features differ from pose landmarks


2.3.4	Summary of Literature Findings
The reviewed studies demonstrate that Random Forest (RF) classifiers are highly effective for recognizing and classifying human movement patterns from pose or motion-related data. Across domains such as sports performance analysis, gait assessment, and activity recognition, RF approaches consistently achieve strong accuracy while maintaining low computational complexity. This makes them suitable for tasks requiring interpretability, robustness to noise, and efficiency.
In sports analysis, prior work shows that RF classifiers can learn discriminative temporal and kinematic patterns from pose-derived or motion capture features without relying on deep neural networks. Studies in golf swing analysis and athletic movement recognition confirm that engineered features combined with RF models deliver high-performance classification, which is particularly relevant to this thesis. It supports the use of feature-based methods over resource-heavy deep learning models that require large datasets.
Research on gait detection and sensor-based activity recognition further highlights RF’s adaptability to heterogeneous inputs and limited training data, aligning with this project’s constraints of mixed public and smartphone video sources. Although some studies use wearable sensors or lab-grade motion capture, their pipelines validate RF’s ability to map biomechanical features to meaningful movement states. These findings reinforce the decision to employ RF for boxing movement analysis, extending prior work by applying RF to camera-derived 3D pose features and aggregating outputs for session-level performance insights in a mobile, camera-only application. Table 2.10 consolidates findings from random forest studies.
 
Table 2.10	Comparative Study of Random Forest Literature Review
Source	Main Findings	Methodology	Strengths	Weaknesses	Relevance to Project
Park et al.  (2017)
3D Human Pose Estimation Algorithm Using Single Depth Images for Pose Analysis	Demonstrated that Random Forest classification improves accuracy and efficiency in sports pose analysis.	Pose and motion feature extraction followed by RF-based classification; validated through experiments and field study.	High accuracy; efficient classification; suitable for sports skill analysis.	Focused on golf; relies on structured capture conditions.	Supports the use of RF with pose-derived features for sports movement classification.
Prodanović et al. (2024)
RF for Identifying Abnormal Patterns of Knee Joint Movement	Showed that RF can precisely classify abnormal gait patterns using motion capture data.	Motion capture feature extraction with RF classifier; evaluated using standard classification metrics.	Robust performance; high precision and recall; interpretable model.	Based on gait analysis, not athletic striking movements.	Demonstrates RF robustness for biomechanical pattern recognition with limited datasets.
Ahmadi et al. (2014) Automatic Activity Classification and Movement Assessment 	Achieved up to 98% accuracy identifying movement patterns using RF with inertial sensor data and DWT features.	Wearable inertial sensors, signal processing (DWT), and RF classification.	Very high accuracy; effective feature-based learning.	Requires wearable sensors; not camera-based.	Confirms RF effectiveness for movement classification.

 
2.4	Comparative Analysis of Existing Applications
2.4.1	Jabbr (AI Boxing Judge) 
Jabbr is a mobile application designed to function as an automated “AI boxing judge,” analysing recorded footage to generate punch statistics and classify punch types. Its primary contribution lies in providing statistics using 2D computer vision models, allowing athletes and coaches to review offensive output without the need for specialized hardware. Jabbr’s use in a gym is as shown in figure 2.1.

 
Figure 2.1	Jabbr use in a gym.

Despite its strengths, Jabbr’s technical scope remains narrow. The application focuses almost exclusively on 2D punch event metrics, such as punch count, punch speed, and punch category. This limitation is significant because many performance issues in boxing, such as fatigue-related guard drop or deteriorating stance, cannot be detected using 2D punch-event data alone. Furthermore, the app does not provide time-series visualisation of body posture, making it difficult for users to examine form consistency across multiple rounds or extended sessions.
For this reason, the proposed project improves upon Jabbr by integrating 3D pose estimation, skeleton playback, and timeline-based visual analytics. By reconstructing and visualizing the boxer’s posture frame-by-frame, the new system enables deeper biomechanical insights including guard height trends, full-body coordination, and continuous fatigue markers. Thus, while Jabbr establishes baseline punch analytics, it does not meet the need for comprehensive, posture-aware boxing performance evaluation. Table 2.11 summarizes this app.
Table 2.11	Jabbr Analysis
App Name	Functionality	Strengths	Weaknesses
Jabbr	AI boxing judge that analyzes footage to produce punch stats and classify punch types.	- Accurate punch recognition. 
- Modern, polished UI aimed at athletes/coaches. 	- Focused mainly on 2D metrics such as counts and speed rather than full-body posture.


2.4.2	Growl (Smart Boxing Ecosystem) 
Growl represents a hardware-integrated boxing training system combining wearable inertial sensors, a smart punching pad, and a companion mobile application. Its architecture provides high measurement accuracy because the underlying sensing system is based on accelerometers, gyroscopes, and force sensors embedded directly into the equipment. This allows Growl to generate precise metrics such as punch force, punch speed, and impact power in real time, making it particularly suitable for competitive athletes or data-driven coaching environments. The ecosystem also includes visual dashboards and AI-enhanced coaching modules, creating a unified technological platform for training. These are shown in figure 2.2.

  
Figure 2.2	Growl’s smart boxing pad, and motion-tracking.

However, Growl’s reliance on proprietary hardware imposes substantial cost and accessibility barriers. Users must purchase the smart pad and sensor-equipped devices, which limits adoption among casual athletes, students, and individuals training outside of gym environments. Users cannot record training sessions with a standard smartphone camera. Additionally, because the system is focused on punch–impact interaction, it provides little insight into form degradation, guard mechanics, or full-body posture changes.
The proposed system addresses these limitations by offering a purely camera-based, hardware-free alternative. Through AI-driven pose estimation, it aims to replicate several Growl-like metrics, such as punch count and movement intensity, without requiring sensors or dedicated equipment. By doing so, it extends accessibility while also enabling analysis of broader biomechanical features such as guard height and footwork patterns, which Growl does not monitor. Thus, this project complements and expands upon Growl’s capabilities while removing its hardware constraints. Table 2.12 summarizes this app.
Table 2.12	Growl Analysis
App Name	Functionality	Strengths	Weaknesses
Growl	Smart training ecosystem combining wearable sensors, smart punching pad, and an app that measures power, speed, and volume. Also features dashboards and AI coaching.	- High accuracy from dedicated hardware sensors. 
- Strong real-time sensor telemetry for metrics like force.	- Requires expensive proprietary hardware.



2.4.3	Tempo (AI Home Gym System) 
Tempo is an AI-powered home fitness system that employs advanced 3D sensing technologies to deliver personalized strength-training and full-body workout guidance. The platform offers rep counting, form scoring, technique correction, and long-term progression tracking, achieving high accuracy due to its structured sensing environment. Its integration of 3D skeletal modeling allows it to detect biomechanical errors and provide real-time corrective cues, which has contributed to its strong reception among home-fitness users seeking high-end coaching experiences. This is shown in figure 2.3. The system is also supported by tailored workout plans, polished visual analytics, and premium hardware design.
 
Figure 2.3	Tempo’s AI tracking and coaching.

Despite these strengths, Tempo exhibits major limitations in relation to accessibility and sport specificity. First, the system requires dedicated 3D sensing hardware, which significantly increases cost and restricts mobility. Second, its subscription-based model presents an ongoing financial commitment. Third, the system is tailored for general fitness movements such as squats, deadlifts, and presses, meaning it lacks recognition of boxing-specific patterns like rotational strikes, guard maintenance, upper body rhythm, and footwork-related movement variability.
The proposed project diverges from Tempo in its goal of enabling smartphone-only 3D pose tracking, without dedicated hardware, while focusing specifically on the biomechanics of boxing. Unlike Tempo, the proposed system will incorporate boxing-specific event detection, guard position analysis, and multi-round trend visualisation. By narrowing the domain to striking sports, this project aims to produce a more specialized and accessible system while still providing 3D visualization and posture-based analytics inspired by high-end platforms like Tempo. Table 2.13 summarizes this app.
 
Table 2.13	Tempo Analysis
App Name	Functionality	Strengths	Weaknesses
Tempo	Home-gym equipment AI coaching app. Provides guided workouts with 3D sensing and real-time form feedback, personalized plans, rep counting and form-scoring.	- Strong AI + 3D sensing for accurate rep/form feedback. 
- Polished coaching experience and personalization. 
- Effective at giving corrective cues and progress tracking.	- Relies on expensive proprietary hardware 
- Subscription model and not mobile-only.
- Not tailored to boxing or general sports


2.4.4	Comparative Analysis Summary 
The analysis of Jabbr, Growl, and Tempo shows that existing commercial systems have each advanced AI-assisted sports training in meaningful ways, yet none fully address the combined needs of accessibility, 3D posture monitoring, and boxing-specific temporal form analysis. Jabbr offers punch analytics but lacks posture tracking; Growl offers precise telemetry but requires costly hardware; and Tempo provides advanced 3D feedback but is limited by proprietary equipment and its focus on general fitness movements.
These gaps confirm the need for a mobile-only, AI-driven boxing performance tracker capable of extracting 3D landmarks, visualizing posture over time, and generating meaningful performance metrics without external devices. Table 2.14 provides a consolidated comparison of Jabbr, Growl, and Tempo.
 
Table 2.14	Comparative Analysis of Existing Apps
App Name	Functionality	Strengths 	Weaknesses 	Proposed Improvements
Jabbr	AI boxing judge that analyzes footage to produce punch stats and classify punch types.	- Accurate punch recognition. 
- Modern, polished UI aimed at athletes/coaches. 	- Focused mainly on 2D metrics such as counts and speed rather than full-body posture.	Provide camera-only 3D skeleton viewer with timeline so users can visually compare pose over time.
Add form-degradation analytics such as guard drop over rounds
Growl	Smart training ecosystem combining wearable sensors, smart punching pad, and an app that measures power, speed, and volume. Also features dashboards and AI coaching.	- High accuracy from dedicated hardware sensors. 
- Strong real-time sensor telemetry for metrics like force.	- Requires expensive proprietary hardware.
	Offer camera-only alternative that replicates many metrics like punch count, volume, and  AI tracking without hardware. 
Tempo	Home-gym equipment AI coaching app. Provides guided workouts with 3D sensing and real-time form feedback, personalized plans, rep counting and form-scoring.	- Strong AI + 3D sensing for accurate rep/form feedback. 
- Polished coaching experience and personalization. 
- Effective at giving corrective cues and progress tracking.	- Relies on expensive proprietary hardware 
- Subscription model and not mobile-only.
- Not tailored to boxing or general sports	Provide a mobile-only version that uses phone camera yet still offers 3D skeleton viewer and form degradation analytics.
Add boxing-specific event detection.

2.5	Data Analysis from Pilot Study
To complement the literature review and comparative app analysis, a pilot study was conducted to understand the training habits, feedback methods, and expectations of potential end-users. An online questionnaire was distributed across university students, recreational fitness groups, and boxing communities. A total of 40 responses were collected. 

2.5.1	Training Demographics, Insights from Respondents, and Feature Interest
As shown in figure 2.4, the respondent pool was diverse in both training experience and sport involvement, allowing for a representative understanding of general user needs for an AI-assisted performance-tracking application.

 
Figure 2.4	Survey Demographic.

The demographic results reveal that 82.5% of respondents train regularly, demonstrating a predominantly active user base. Of those who train, 33.3% participate in boxing or other striking sports, while an additional 42.4% engage in form-focused disciplines such as strength training and dance. This composition is highly relevant because sports involving repeated movements and technique refinement naturally benefit from visual feedback systems and motion analysis tools. Furthermore, 51.5% of respondents train 3–6 times per week, indicating consistent routines in which fatigue and form degradation are likely to occur over extended sessions. These insights show that the sample population is well aligned with the intended target audience for the proposed system, as they are both engaged in performance-based activities and motivated to improve training quality.

 
Figure 2.5	Feedback and Review Methods.

Despite high training frequency, athletes reported limited access to reliable feedback mechanisms. As shown in figure 2.5, only 27.3% of respondents receive in-person coaching, highlighting the scarcity of professional guidance, especially in non-gym or casual settings. Meanwhile, 33.3% rely on video self-review, which is subjective, time-consuming, and dependent on individual interpretation. A significant 45.5% of participants receive no feedback at all, underscoring a major gap in accessible performance evaluation tools.
A particularly important finding is that all boxers in the survey record their training sessions, but only 18% have these recordings reviewed by a coach. This confirms that while visual data is available, it is not being effectively analyzed. This gap reinforces the need for an automated system capable of extracting meaningful insights, such as guard maintenance, punch volume, and movement patterns, directly from video input.

 
Figure 2.6	Non-trainers.

Non-trainers also provided valuable perspectives. As shown in figure 2.6, the study found that 71.4% identify lack of affordable coaching as a barrier to starting exercise, while 57.1% cite lack of training knowledge. Importantly, 85.7% indicated that the proposed mobile application could encourage them to begin training. This suggests that an accessible, camera-based performance analysis tool could lower psychological and financial barriers to entry by offering clear, objective feedback without requiring professional supervision.

 
Figure 2.7	Interest in features.

As shown in figure 2.7, the pilot study also assessed user interest in specific app features. While 55% reported difficulty identifying mistakes in training form, a much larger proportion expressed interest in structured visual analytics:
•	82.5% want to track fatigue or form degradation trends,
•	70% want a 3D replay of their training, and
•	87.5% want visual graphs summarizing performance changes over time.
The data indicates a strong preference for visual, easy-to-interpret analytical tools rather than traditional numeric statistics. Additionally, among athletes, especially boxers, there was 100% interest in form degradation tracking and fatigue-related monitoring.

2.5.2	Identified User Needs and Implications for System Design
From the analysis, several key user needs are identified:
•	Automated movement and form feedback without reliance on professional coaching.
•	Visual analysis features, particularly 3D replays and performance graphs.
•	Trend tracking capabilities to help identify degradation due to fatigue over extended training sessions.
•	Lower barriers to entry for beginners, who require simple, accessible guidance.
•	Affordable alternatives to wearables, sensors, or proprietary fitness hardware.
These needs directly inform the functional requirements of the proposed system.

Based on user responses, the following features are prioritized for inclusion:
•	AI-based pose estimation and a 3D skeleton viewer powered by smartphone camera input.
•	Timeline playback for reviewing form changes throughout a session.
•	Graphical dashboards illustrating punch volume, movement frequency, and guard trends.
•	Form degradation analysis derived from temporal patterns in wrist, shoulder, and hip coordinates.
•	Video recording and comparison tools for reviewing multiple sessions side-by-side.
These components reflect the intersection of user expectations, technical feasibility, and the identified gaps in existing literature and commercial applications.

2.5.3	Pilot Study Summary
The pilot study confirms that both frequent athletes and occasional exercisers experience major limitations in obtaining clear, objective performance feedback. With over 90% of participants expressing strong interest in AI-driven movement analysis and 3D visual tracking, the findings strongly validate the need for the proposed system. A mobile, camera-only solution capable of visualizing full-body motion and generating trend-based analytics aligns with user expectations and addresses persistent challenges in accessibility, affordability, and technical feedback availability. Accordingly, these insights directly guide the design and development priorities of the boxing performance-tracking application.

 
2.6	Summary 
The literature review confirms that AI-based pose estimation and camera-only motion analysis have advanced significantly, enabling accurate landmark extraction and biomechanical assessment without specialized hardware. Studies highlight the effectiveness of CNN-based models for markerless pose estimation and Random Forest classifiers for movement classification, offering strong accuracy with low computational cost. These findings provide a solid foundation for developing accessible, mobile-based sports analysis tools.
Despite these advancements, major gaps remain. Existing research focuses on isolated tasks such as punch detection or repetition counting, while commercial systems like Jabbr, Growl, and Tempo depend on proprietary hardware or limit analysis to basic 2D metrics. None deliver a camera-only solution capable of full-body 3D visualization and session-level trend analysis for boxing. This underscores the need for a cost-effective mobile application that integrates pose tracking, temporal analytics, and interactive visualization.
The pilot study reinforces this need, with over 90% of participants expressing interest in fatigue tracking, 3D replays, and automated feedback. These insights validate the demand for an AI-assisted, smartphone-based tool that delivers comprehensive performance analysis with minimal setup.
Therefore, this thesis addresses the identified research gap by proposing a mobile application that integrates pose estimation, Random Forest-based classification, interactive 3D visualization, and time-based performance analytics for boxing training. The next chapter outlines the methodology and system architecture developed to achieve these objectives.
 
