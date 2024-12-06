# Hearing Aid Android App Project

This project aims to provide an affordable and accessible solution for individuals with hearing impairments by transforming standard earphones with microphones into a hearing aid alternative. The Android application utilizes advanced audio processing techniques, including real-time audio monitoring, noise cancellation, and equalizer integration, to deliver a functional and user-friendly experience for users with hearing loss.

## Project Overview

The goal of this project is to develop a mobile application capable of enhancing the hearing experience by leveraging commonly available consumer hardware (earphones or headphones with a microphone). The app focuses on two key aspects:

- **Audio Monitoring**: Real-time monitoring of the ambient sound through the microphone and playback through the earphones, providing an experience similar to that of traditional hearing aids.
- **Noise Cancellation & Speech Detection**: The app includes an AI-driven noise cancellation feature powered by the VadSilero speech detection model, which helps to isolate speech from background noise, making it more intelligible for users in noisy environments.

While this project is a proof-of-concept, it demonstrates the potential of using mobile devices to support hearing-impaired individuals at a fraction of the cost of traditional hearing aids. The application also serves as a research tool to identify key challenges, such as audio latency, which are crucial for further development of this type of technology.

## Key Features

- **Real-Time Audio Monitoring**: The app processes incoming audio and plays it back in real-time with low latency, helping users hear their environment more clearly.
- **Equalizer Presets**: The app includes several audio equalizer presets to fine-tune the audio output for optimal hearing enhancement.
- **AI-Powered Noise Cancellation**: A speech detection model (VadSilero) is used to detect and isolate speech from noise, reducing background distractions.
- **Latency Measurement**: The app includes a latency measurement feature that tracks the delay between audio input and playback, providing real-time feedback on performance.
- **Customizable Audio Settings**: Users can adjust the audio equalizer to suit their preferences, with control over frequency bands.

## Tools and Technologies Used

- **Android Studio**: Integrated development environment (IDE) for building and testing the Android application.
- **Kotlin**: Primary programming language used for app development.
- **VadSilero AI Library**: Used for speech detection and noise cancellation.
- **Equalizer**: Android's built-in equalizer API for customizing audio output.
- **AudioRecord and AudioTrack**: Android APIs for capturing and playing audio in real-time.
- **XML**: Used for designing the user interface (UI) of the application.

## Project Challenges and Insights

This project was developed to explore the feasibility of providing affordable hearing assistance using widely available mobile devices. Several technical challenges were encountered, including:

- **Latency**: A major challenge was achieving low enough audio latency for the app to be practical as a hearing aid. Despite efforts to optimize the app and reduce latency, limitations in mobile device processing speed and security protocols made it difficult to meet the required standards for real-time audio monitoring.
- **Noise Cancellation**: Implementing effective noise cancellation was another hurdle. The use of VadSilero's AI-based speech detection algorithm helped minimize background noise, but the technology’s impact on overall latency was still significant.

The project serves as a foundation for future research and development in the area of mobile-based hearing aids, offering valuable insights into the technical hurdles and potential solutions in this space.

## Conclusion

This Android app demonstrates the potential of leveraging mobile devices for hearing enhancement. It provides a platform for further innovation in the field, particularly for those who cannot afford traditional hearing aids. The project highlights key challenges, including latency and noise processing, which are critical to the success of mobile hearing solutions. Despite the technical challenges, the app offers a promising direction for making hearing aids more accessible and affordable.
