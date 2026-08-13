# Appendices

Appendix A is reused from `old versions/old thesis/7_old_append.md`.
Formatting was adjusted for consistency with the rest of this thesis, but
the wording and options are unchanged. Appendix B is the new
user-evaluation instrument and results, reused from
`thesis/notes/tester_questionnaire.md`.

_Status: NEW. Created to hold both questionnaires as full appendix content
instead of leaving them as loose notes files._

## Appendix A: Pilot Study Questionnaire

**Title:** AI-Driven Sports Performance Tracker – Pilot Study Questionnaire

**Purpose:** To gather user insights on training habits, feedback methods,
and interest in AI based performance tracking features.

### Section 1: Demographics

**What is your age group?**
Under 18 | 18–24 | 25–34 | 35–44 | 45 and above

**Which best describes your involvement in sports or fitness?**
Casual hobbyist | Regular fitness enthusiast | Amateur athlete |
Professional coach/trainer | I don't currently train

### Section 2: Training Habits

**What is your primary sport or activity?**
Boxing / Striking sports | Gym / Strength training | Cardio / General
fitness | Yoga / Rehabilitation | Other

**How often do you train per week?**
1–2 times | 3–4 times | 5–6 times | 7+ times

**When training alone, how do you usually get feedback on your form?**
(Select all that apply)
Video self-review | In-person coach | Wearable sensors | Mobile app | I
don't get feedback | Other

**Do you currently record your training sessions?**
Always | Sometimes | Never

**If yes, how do you usually review them?** (Select all that apply)
Watch full session | Watch slow motion | Ask coach/trainer | Don't review |
Other

### Section 3: Non-Trainers

**What are the main reasons you don't currently train or exercise
regularly?** (Select all that apply)
Lack of time or motivation | No access to affordable coaching | Don't know
proper techniques | Injuries or health issues | Other

**Would a simple mobile app that can analyze your movement encourage you to
start training?**
Yes | Maybe | No

**What kind of training would interest you most if AI feedback were
available?**
Boxing / Striking sports | Gym / Strength training | Cardio / General
fitness | Yoga / Rehabilitation | Other

### Section 4: Challenges, Needs & Desired Features

Each statement below was rated on a 1 (Disagree) to 5 (Agree) scale.

- I find it difficult to identify or correct mistakes in my training form
  without professional feedback.
- I want to track fatigue or form degradation trends over time to see how
  my performance changes.
- I would enjoy a 3D replay of my training sessions showing my body
  movement and form over time.
- I would like to see a clear, visual summary of my performance after each
  session (e.g., activity volume, movement frequency, form degradation).

**What additional features or improvements would you like to see in this
app?** (Optional, free text)

## Appendix B: Tester Questionnaire and Results

This instrument was handed to each boxer immediately after their
recording/playback session, while the experience was fresh. It feeds the
user-centered evaluation reported in §5.7.

Each respondent recorded a tester ID (first name or initials, not
published, used only to match answers back to the device/scenario log),
boxing experience level (Beginner, Intermediate, or Advanced/Competitive),
and stance (Orthodox or Southpaw).

### Part 1: Rated Statements

Each statement was rated on a 1 (strongly disagree) to 5 (strongly agree)
scale.

1. It was easy to start and stop a recording.
2. I understood what the app was doing while it processed my video.
3. The skeleton overlay on the video was easy to follow.
4. The graphs (guard height / movement / punch count) were easy to
   understand.
5. The punch count roughly matched what I actually threw.
6. The guard height graph reflected what I actually did with my guard.
7. The movement graph reflected how much I actually moved around.
8. I trust this feedback enough to use it to check my technique.
9. The app felt reliable, with nothing crashing, freezing, or looking
   obviously broken.
10. Overall, the app was easy to use.

### Part 2: Open Questions

1. Was there anything on screen you did not understand or were not sure
   how to read?
2. Did anything in the results look wrong or surprise you (a punch count, a
   graph, the skeleton tracking)? Describe what happened.
3. If you were reviewing your own training footage, what is the single
   most useful thing this app could show you that it does not right now?
4. Anything else, confusing, annoying, broken, or that you liked?

With only three testers this is not a statistically powered usability
study. Scores below are reported as directional, and the write-up in §5.7
leans on the open answers and the observed failures from the
device/scenario log rather than on averaging the Likert scores as if they
were significant.

### Results (Collected 2026-08-10)

Collected verbally immediately after each boxer's session.

**Part 1: Likert ratings (1–5)**

| # | Statement | Boxer A | Boxer B | Boxer C |
|---|---|---|---|---|
| 1 | Easy to start/stop a recording | 5 | 5 | 5 |
| 2 | Understood processing state | 5 | 5 | 4 |
| 3 | Skeleton overlay easy to follow | 5 | 5 | 5 |
| 4 | Graphs easy to understand | 5 | 5 | 5 |
| 5 | Punch count matched what was thrown | 4 | 3 | 5 |
| 6 | Guard height reflected actual guard | 5 | 5 | 5 |
| 7 | Movement reflected actual movement | 5 | 5 | 5 |
| 8 | Trust feedback enough for technique review | 5 | 4 | 5 |
| 9 | App felt reliable | 5 | 5 | 5 |
| 10 | Overall easy to use | 5 | 5 | 5 |

**Part 2: Open answers**

| Q | Boxer A | Boxer B | Boxer C |
|---|---|---|---|
| 1. Anything unclear? | No | No | No |
| 2. Anything look wrong/surprising? | No | Punches not detected | No |
| 3. Most useful thing to add? | Punch types | Idk | Punch type |
| 4. Anything else? | None | None | None |

Boxer A and Boxer C, both orthodox and both associated with the higher
recall figures reported in §5.5, reported nothing unclear or wrong and
independently asked for a punch-type breakdown as the next feature. Boxer
B, southpaw and associated with the lowest recall figure, reported the
missed-punch issue directly and had no feature request. §5.7 reads this
together as an accuracy-before-features pattern.
