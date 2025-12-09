# VisualSLAMdroid — Visual SLAM & Odometry for Android

DEADR is a lightweight Android application designed to perform **camera-based dead reckoning** and implement **basic visual SLAM (Simultaneous Localization and Mapping)**. It captures camera frames, extracts visual features, estimates the device's motion between frames, and visualizes the resulting trajectory in real-time.

---

## Features

* **Camera Integration:** Camera frame capture using Android's Camera2 or CameraX APIs.
* **Feature Detection & Matching:** Utilizes standard algorithms like **ORB** or **FAST** for keypoint detection and descriptor matching between consecutive frames.
* **Motion Estimation:** Calculates frame-to-frame relative camera motion using techniques like the **Essential Matrix** or **PnP (Perspective-n-Point)**.
* **Trajectory Tracking:** Accumulates estimated poses to build a real-time device trajectory.
* **Basic SLAM Hooks:** Includes foundational utilities for building a full SLAM system (e.g., pose graph structure, readiness for loop-closure detection).
* **Real-time Visualization:** Renders the computed trajectory on the device screen in real-time.
* **Optional IMU Integration:** Supports integration of Inertial Measurement Unit (IMU) data to potentially improve scale estimation and overall pose accuracy.
* **Simple UI:** Provides a straightforward interface to start/stop the tracking process and export logs.

---

## How It Works (Visual Odometry Pipeline)

The core functionality follows a classic Visual Odometry (VO) pipeline:


1.  **Frame Capture & Preprocessing:** Camera frames are captured continuously and preprocessed (e.g., converted to grayscale, resized).
2.  **Feature Extraction & Matching:** Keypoints and descriptors are extracted from the current frame and matched with features from the previous frame.
3.  **Relative Motion Estimation:** The relative camera pose (rotation and translation) between the matched frames is estimated (e.g., using the Essential Matrix).
4.  **Trajectory Accumulation:** The newly estimated motion is applied to the previous pose to calculate the device's current absolute pose, building the overall trajectory.
5.  **SLAM Refinement (Optional):** If enabled, IMU data and pose graph updates can be used to refine and globally optimize the accumulated motion and pose estimates.

---

## Project Structure

app/
├── camera/ # Camera handling & frame callbacks (Camera2/CameraX setup)
├── processing/ # Frame preprocessing (grayscale, resize, distortion correction, etc.)
├── features/ # Keypoint detection & descriptor matching (ORB/FAST implementation)
├── vo/ # Visual Odometry logic (frame-to-frame pose estimation)
├── slam/ # Pose graph & SLAM utilities (loop-closure preparation)
├── logging/ # Run logging (poses, IMU data, debug info)
└── ui/ # App UI & trajectory rendering

---

## Setup & Build

### 1. Clone the Repository
    git clone https://github.com/AbhishekGuna/DEADR.git

### 2. Open in Android Studio
Open the cloned project directory in Android Studio.

### 3. Build & Run
Select the **app** module and run it on a physical Android device.

---

## Exported Logs

### Estimated Poses
- 6-DOF pose data (position + orientation) recorded over time.

### IMU Data
- Raw accelerometer and gyroscope readings (if IMU integration is enabled).

### Match Statistics
- Number of detected features, matched pairs, inliers, and other VO metrics.
