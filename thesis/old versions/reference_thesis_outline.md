# Reference thesis outline (similar project)

A comparable boxing-performance-tracking thesis, kept for structural reference
— particularly Chapter 4's split between a desktop/Python prototype and the
Android adaptation, which is the shape the "System Development" chapter in
this thesis borrows.

```
1 Introduction 1
1.1 Background 1
1.2 Problem 2
1.2.1 Original problem and definition 2
1.2.2 Scientific and engineering issues 3
1.3 Purpose 3
1.4 Goals 4
1.5 Research Methodology 5
1.6 Delimitations 5
1.7 Structure of the thesis 5

2 Background 7
2.1 Overview of Boxing and Boxing Techniques 7
2.1.1 Key boxing technique 8
2.1.2 Punches 8
2.1.2.1 Jab 8
2.1.2.2 Cross 8
2.1.2.3 Hook 9
2.1.2.4 Uppercut 9
2.1.3 Guard types 10
2.1.3.1 Guard up 10
2.1.3.2 Guard Down 10
2.1.3.3 Long guard 10
2.1.4 Intentional and Unintentional Guard Positions 10
2.2 Machine Learning 11
2.2.1 Neural Network 12
2.3 Human Pose Estimation Technologies 12
2.3.1 Human Pose Estimation 12
2.3.2 Human Pose Estimation Challenges 13
2.3.3 Mediapipe Human Pose Estimation 14
2.3.3.1 MobileNetV2 14
2.3.3.2 GHUM 14
2.3.3.3 BlazePose 14
2.3.4 Tensorflow 17
2.3.4.1 TensorFlow Lite 17
2.3.5 Scikit learn 17
2.4 Related work area 18
2.4.1 Cricket performance analysis 18
2.5 Summary 18

3 Method or Methods 20
3.1 Research Process 20
3.2 Research Paradigm 22
3.3 Data Collection 23
3.3.1 Sampling 23
3.3.2 Sample Size 24
3.3.3 Target Population 25
3.4 Experimental design/Planned Measurements 25
3.4.1 Test environment/test bed/model 25
3.4.2 Hardware/Software to be used 26
3.5 Assessing reliability and validity of the data collected 26
3.5.1 Validity of method 26
3.5.2 Reliability of method 26
3.5.3 Data validity 27
3.5.4 Reliability of data 27
3.6 Planned Data Analysis 28
3.6.1 Data Analysis Technique 28
3.6.2 Software Tools 30
3.7 Evaluation framework 30

4 Development of system 32
4.1 Choice of Human Pose Estimation technology 32
4.2 Desktop prototype 33
4.2.1 Extracting the coordinates 34
4.2.2 Defining the guard status thresholds 34
4.2.3 The feedback algorithm 35
4.2.4 Training the Machine Learning model 36
4.3 Android application 38
4.3.1 Adapting the system for Android 39
4.3.2 Differences between desktop prototype and android application 39
4.3.2.1 Tensorflow lite models 39
4.3.2.2 Timer implementation 40
4.3.2.3 Guard status thresholds 41

5 Results and Analysis 42
5.1 Major results 42
5.1.1 Results, analysis and discussion of controlled test 42
5.1.1.1 Results for each guard status in the controlled tests 43
5.1.1.2 Overall system performance evaluation 44
5.1.2 Results, analysis and discussion of participant survey 49
5.2 Reliability Analysis 59
5.2.1 Reliability of method 59
5.2.2 Reliability of data 60
5.3 Validity Analysis 61
5.3.1 Validity of method 61
5.3.2 Validity of data 62

6 Discussion 63
6.1 Research method 63
6.1.1 Evaluation of the Chosen Methodology 63
6.2 Mobile phone application 64
6.2.1 Discussion on Mediapipe and Alternative Technologies 64
6.2.2 Pose detection challenges 64
6.2.3 Sound feedback 66
6.2.4 Suggestions for improvement 66

7 Conclusions and Future work 69
7.1 Conclusions 69
7.2 Limitations 70
7.3 Future work 71
7.3.1 Next things to be done 71
7.4 Reflections 71

References 73
```
