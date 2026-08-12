CHAPTER 1
INTRODUCTION
1.1	Background
Traditionally, sports training has relied on manual observation by coaches or athletes, which can be inconsistent, time-consuming, and prone to human error (Stenum et al., 2021). In striking sports such as boxing, maintaining proper form is important for both performance and injury prevention. However, as fatigue sets in during training, athletes often experience form degradation without realizing it.
With recent advancements in artificial intelligence (AI) and deep learning, pose estimation models now make it possible to analyze human movement directly from standard video input. This eliminates the need for motion capture suits or specialized tracking hardware, making the technology cost-effective and accessible. Although research has demonstrated the potential integration of AI-based pose estimation into sports environments, there are currently few mobile solutions that provide real-time, camera-only performance tracking tailored to individual athletes.
Therefore, this project aims to develop a mobile application for athletes, both casual and professional, that records training sessions using the smartphone camera and applies 3D pose estimation combined with Random Forest classification to track body movements. The system will provide immediate insights into form accuracy, activity levels, and fatigue trends, thereby supporting effective training and performance improvement.
1.2	Problem Statement
Public interest in health, exercise, and sports has increased significantly in recent years, driven by growing awareness of health and well-being (Chern, 2025). Many individuals incorporate physical training into their daily routines. However, effective sports training requires substantial technical knowledge, which may not be widely available or may demand commitments beyond the reach of casual athletes.
Currently, athletes most often rely on manual observation by coaches or wearable sensors for performance feedback. Standard biomechanical analysis methods typically involve laboratory-based facilities and expert personnel, which are both time-consuming and expensive (Souaifi et al., 2025). Existing consumer fitness applications and smart devices that track movement mainly focus on basic metrics such as repetition counts or one-time form checks. For example, Forbes highlights a swing analysis app for tennis that evaluates single-movement performance (Chan-Danisi, 2024). However, these systems lack time-based analysis of form degradation or fatigue trends that occur over prolonged training sessions, such as in boxing.
AI adoption in athlete training is increasing across ASEAN countries, but its implementation in Malaysia’s sports sector remains in its early stages. Nevertheless, studies show growing trust among athletes toward AI-assisted tools in Malaysia, with overall perceptions of AI reported as high (mean = 3.69110) (Nazrin Aiman Azmi, 2025).
Therefore, there is a clear need for a low-cost, mobile solution that provides analysis of technique, fatigue indicators, and training volume, making high-quality performance feedback accessible to a wider audience.
1.3	Research Question
This study is guided by the following research questions:
i)	How can AI-based 3D pose estimation be adapted to analyze boxing-specific movements using only a smartphone camera?
ii)	What methods can be implemented to detect fatigue-related form degradation across multiple training rounds using Random Forest classification?
iii)	How effective is the proposed mobile application in providing clear, actionable feedback compared to existing fitness apps and manual observation?
1.4	Research Objective
The main objective of this research is to develop a mobile application that uses AI-driven 3D pose estimation and Random Forest classification to track boxing performance and provide visual summaries. Specifically, the objectives are as follows:
i)	To investigate existing pose-estimation systems for sports, feature representations for boxing, and user requirements through literature review and a pilot study.
ii)	To develop a mobile application prototype that combines video capture, AI‑based pose estimation, and machine learning classification to track boxing movements and fatigue, then present the results through intuitive visualizations for the user.
iii)	To evaluate the system’s effectiveness and usability by validating against a self-collected smartphone boxing dataset and user feedback.
1.5	Scope of Research
This project focuses on developing a mobile application prototype for boxing performance tracking. The scope of this research are as follows:
•	Video capture and frame extraction using a smartphone camera.
•	On-device pose estimation using CNN-based models such as MediaPipe BlazePose.
•	Extraction of 3D body landmarks (head, shoulders, elbows, wrists, hips, knees, ankles, feet).
•	Analysis of boxing-specific metrics, including:
o	Guard degradation (hand height relative to face when not punching).
o	Punch volume (number of punches per minute).
o	Movement frequency (general body movement besides punching).
•	Application of Random Forest classification to detect fatigue-related form degradation trends.
•	Visualization of results through a 3D skeleton viewer, interactive timeline, and graphical dashboards.
•	Validation using public datasets during development and a self-collected smartphone dataset during testing.
Out-of-scope items include real-time multi-person tracking, multi-camera synchronized motion-capture workflows, or integration with commercial wearable hardware and depth sensors. High-precision laboratory motion capture systems and clinical diagnostic tools for medical use are excluded, as are applications in boxing sparring sessions or competitive matches where motion and visibility are unpredictable. Other exclusions include training for sports outside the defined boxing ruleset, payment gateways, enterprise gym management system integration, live streaming to remote coaches, and automated medical or injury diagnosis.
The system is designed for single-user sessions captured by a single smartphone camera under reasonable lighting conditions with minimal occlusion to ensure accurate landmark extraction. It assumes stationary boxing training involving a punching bag rather than dynamic sparring scenarios.

 
1.6	Thesis Outline
Based on the objectives previously presented and on the approach proposed before, this thesis is made up of five (5) chapters, which contents are summarized as follows:
•	Chapter 1. Introduction. This chapter presents the background of the study, research problems, objectives, scopes, contributions and significance of the research.
•	Chapter 2. Literature review. This chapter starts reviews existing research on AI-based pose estimation, biomechanics in sports, and related applications in boxing and fitness.
•	Chapter 3. Methodology. This chapter describes the research design, system architecture, tools, and technologies used to develop the mobile application. 
•	Chapter 4. Result and Discussion. This chapter provides analysis of the system’s performance, validation results, and user feedback.
•	Chapter 5. Conclusion and Recommendations for Future Research. This chapter summarizes the main conclusions as well as achievements of the work undertaken in this research and suggests areas for future work.
 
