# Field test protocol — C6 (multiple people in frame) & D (cross-device/user variance)

Run with the 3 boxers. Feeds §5.5 (metric validity), §5.6 (error/robustness
testing), §5.7 (user-centered evaluation), and §6.5 (limitations).

**Status (2026-08-10): FINAL — in-person testing is complete, no further
sessions planned.** Part 1 (D) run in full — see results in
`05_results_and_evaluation.md` §5.4/§5.5. Part 2 (C6) — only scenario 3
("two people boxing simultaneously") run, results in §5.6; scenarios 1/2/4
(walk-through, static bystander) were not run and are recorded as a scope
limitation in §6.5, not an open action item. Post-session interview run and
transcribed — results in §5.7, raw scores appended to
`tester_questionnaire.md`. The device-swap option below and the sensitivity
notes elsewhere in this file are left as-written for methodology record but
are no longer actionable.

## Standardized drill (do this for every recording, all scenarios)

Using the same drill across boxers/scenarios is what makes the results
comparable instead of anecdotal, and gives a manual ground-truth punch count
to check the app's punch volume against (dual-purpose with §5.5).

1. 10x jab
2. 10x jab–cross
3. 10x lead hook
4. 30s freestyle combos (whatever feels natural)

One person (you, or a coach) manually tallies punches thrown live or from a
second phone recording, for later comparison against the app's count.

---

## Part 1 — D: Cross-device / user variance

Each boxer runs the standardized drill once, solo, on their **own** phone.

| # | Boxer ID | Phone model | Android version | Stance (ortho/southpaw) | Top worn (bare/shirt/rash guard) | Lighting (gym/window/mixed) | Manual punch count | App punch count | Notes |
|---|----------|-------------|------------------|--------------------------|-----------------------------------|-------------------------------|---------------------|------------------|-------|
| 1 |          |             |                  |                          |                                    |                                |                     |                  |       |
| 2 |          |             |                  |                          |                                    |                                |                     |                  |       |
| 3 |          |             |                  |                          |                                    |                                |                     |                  |       |

Optional, if time allows: have one boxer repeat the drill on a *different*
phone than their own. That isolates device effects from person effects
(currently confounded 1:1 with only 3 phones) — nice to have, skip if the
session is tight.

**What to check per row, beyond the count:**
- Does guard height/movement look sane for that person's build/stance (e.g.
  southpaw guard shouldn't invert or break the metric)?
- Any visible landmark tracking failures tied to clothing/skin
  contrast — note it factually (e.g. "wrist landmark jittered during
  bare-chested recording"), this is exactly the kind of finding worth a line
  in limitations.
- Record who's who (Boxer 1/2/3) so a failure is traceable, but keep the
  actual write-up in the thesis anonymized/aggregated — don't name testers
  in the document.

---

## Part 2 — C6: Multiple people in frame

Four short recordings (~30–45s each is enough, doesn't need the full drill):

1. **Baseline** — one boxer alone in frame, nobody enters. (Control.)
2. **Brief walk-through** — a second person (coach/other boxer) walks across
   the background for 2–3 seconds mid-recording, then leaves frame.
3. **Two people boxing simultaneously** — two boxers both fully in frame for
   the whole clip (e.g. both shadowboxing side by side, or one holding pads).
4. **Static bystander** — a second person stands in frame but doesn't move
   much (e.g. someone watching), for the whole clip.

**What to check per scenario:**
- Whose skeleton does the overlay track — does it stay locked on one person,
  or does it jump between people mid-video?
- Does the punch classifier fire on the *wrong* person's motion (e.g. counts
  punches while the tracked skeleton is actually the bystander)?
- Does tracking recover after the second person leaves frame (scenario 2),
  or does it stay latched onto whoever it picked up?
- Does processing fail/error out entirely, or does it silently produce a
  plausible-looking but wrong result? (Silent-wrong is the worse outcome —
  flag it clearly if you see it.)

---

## Recording the results

For each row/scenario: expected vs. actual, pass/fail, and a screenshot of
the playback screen if something looks off. This log is your raw data for
§5.5/§5.6 — write the actual prose summary in the thesis, not the raw table.
