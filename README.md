# GPU Utilization Management

This is our Java project for the Object-Oriented Programming course at Khalifa University. It simulates four GPUs that students share. Students submit jobs, and the system puts each job on a free GPU or adds it to the waiting list. The admin can approve students, manage their GPU minutes, and advance the simulation clock.

## Group members

| Name | GitHub username |
|---|---|
| Zayed Saeed | `zayedsaeed28-dotcom` |
| Mansour | `MAlshamsi7` |
| Saif | `SaifAlzaaabi1` |
| Ahmed | `Ahmed5117` |

## How to run the project

1. Install Java Development Kit (JDK) 21 on your computer.
2. On the main GitHub page, click **Code → Download ZIP** and extract the ZIP.
3. Open a terminal in the extracted folder that contains `AccessRequest.java`, `src`, and `tests`.
4. Run these commands. The long `javac` command is one line.

```text
mkdir out
javac -d out AccessRequest.java RegistrationManager.java src/ClockFile.java src/DashboardFrame.java src/GPU.java src/Job.java src/Scheduler.java src/SimulationClock.java src/StudentJobForm.java src/TimeAllowance.java tests/ApprovalTest.java
java -cp out StudentJobForm
```

A window opens with **Student login** and **Admin login**.

## Try one job

1. Click **Admin login**. Use `admin` as the username and `gpu2026` as the password.
2. On the first run, choose a date and time, click **Set start time**, then **Log out**.
3. Click **Student login**. Use the demo student ID `S01` and password `demo`.
4. Enter a job title, application name, and description. Select **Small (5 min)** and click **Confirm and submit job**.
5. Click **My jobs** to see the job and its status. Log out and sign back in as the admin.
6. Click **Advance Minute** five times. If the job started right away, its status will change to **Completed**.

Closing the main window saves the accounts, jobs, GPU balances, and clock in `ku-gpu-registrations.dat` in your user home folder. Run `java -cp out StudentJobForm` again to continue from the saved time.

## Run the sample demo

The [sample save](demo/ku-gpu-registrations.dat) opens at **16/11/2026 10:18** with all four GPUs busy. Of the 20 jobs, 7 are completed, 4 are running, 7 are queued, and 2 are cancelled. The [job list](demo/jobs.csv) shows the starting records on GitHub.

1. Compile the project using the instructions above. If the regular app is open, close its main window.
2. In the project folder, create a folder called `demo-run` beside `src`. Copy `ku-gpu-registrations.dat` from the `demo` folder into `demo-run`. Do this once.
3. From the project folder, start the app with this command:

```text
java "-Duser.home=demo-run" -cp out StudentJobForm
```

The app saves changes in `demo-run` when you close its main window. Use the same command to continue. To start again from 10:18, create a fresh demo folder, copy the original sample into it, and use that folder name in the command.

| Demo account | What to try |
|---|---|
| `admin` / `gpu2026` | On the admin dashboard, look at **All jobs**. Job 14 is first in line. Click **Advance Minute** twice. At 10:20 job 8 finishes and job 14 starts on GPU 4. |
| `S01` to `S05` / `demo` | These five students are approved. Sign in as `S01` and open **My jobs**. Job 6 shows an admin cancellation after 3 minutes of runtime. |
| `S04` / `demo` | Job 9 was cancelled by the student while queued. Its charge is 0 minutes. |
| `S05` / `demo` | In the starting save, 5 minutes are used and 95 are reserved. Find `S05` as admin and grant 5 minutes with a reason. The student can then submit one Small job using those 5 minutes. |
| `S06` and `S07` | `S06` is pending and appears in **Review account requests**. `S07` is rejected. Both have the demo password `demo`. |

## Run the approval test

After compiling, run:

```text
java -cp out ApprovalTest
```

The test checks that approving the same student twice keeps their existing balance. The expected output is:

```text
Passed: repeat approval kept the balance at 115 minutes.
```
