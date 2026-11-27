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

## Run the approval test

After compiling, run:

```text
java -cp out ApprovalTest
```

The test checks that approving the same student twice keeps their existing balance. The expected output is:

```text
Passed: repeat approval kept the balance at 115 minutes.
```
