# ecoeye (실시간 쓰레기 분리수거 보조 시스템)

<br>

> *Image*

<br>

## 📌 프로젝트 소개

본 프로젝트는 스마트폰 카메라를 이용해 **실시간으로 쓰레기 분리수거 오류를 탐지**하는 안드로이드 애플리케이션입니다. YOLOv8 딥러닝 모델을 모바일 환경에 최적화하여 탑재하였으며, 일반쓰레기, 캔·병, 종이로 구분된 쓰레기통의 내용물을 비추면 AI가 자동으로 객체를 인식하고 분류 결과를 화면에 보여줍니다.

이를 통해 재활용 효율성을 저해하는 잘못된 분리배출 습관을 개선하고, 효과적인 실내 폐기물 관리에 기여하는 것을 목표로 합니다.

<br>

## ✨ 주요 기능

  * **실시간 객체 탐지**: 카메라 화면에 보이는 쓰레기를 실시간으로 탐지합니다.
  * **3종 쓰레기 분류**: **종이(paper)**, **캔·병(cans\_bottles)**, **일반쓰레기(general\_waste)** 3가지 클래스를 구분합니다.
  * **결과 시각화**: 탐지된 객체 위치에 \*\*바운딩 박스(Bounding Box)\*\*를 그리고, **클래스 이름**과 **신뢰도 점수**를 함께 표시합니다.
  * **모바일 최적화**: 경량화된 TFLite 모델을 사용하여 모바일 기기에서 빠르고 효율적으로 동작합니다.

<br>

## 🛠️ 기술 스택 및 개발 과정

### 🤖 AI 모델 개발

  * **Model**: **YOLOv8s**
  * **Framework**: **PyTorch**, **Ultralytics**
  * **Training Environment**: **Google Colab** (Free GPU 활용)
  * **Dataset**: 웹 크롤링 및 직접 촬영을 통해 구축한 커스텀 데이터셋
  * **Annotation Tool**: **Roboflow** (데이터 라벨링, Train/Valid/Test 분할, 데이터 증강에 활용)

### 📱 모바일 애플리케이션

  * **Platform**: **Android**
  * **Language**: **Kotlin**
  * **Core Libraries**:
      * **CameraX**: 실시간 카메라 프리뷰 및 이미지 스트림 처리
      * **TensorFlow Lite (Task Vision Library)**: 안드로이드 기기 내(On-device)에서 AI 모델 추론 실행

### 🔄 개발 워크플로우

1.  **데이터 수집**: 웹 크롤링과 직접 촬영으로 초기 이미지 데이터셋 확보
2.  **데이터 라벨링**: Roboflow를 사용해 객체에 바운딩 박스 Annotation 진행
3.  **모델 학습**: Google Colab 환경에서 YOLOv8s 모델을 커스텀 데이터셋으로 Fine-tuning
4.  **성능 평가 및 개선**: 학습된 모델의 mAP, Precision, Recall을 평가하고, 부족한 데이터를 보강하며 성능 개선 사이클 반복
5.  **모델 변환 및 최적화**: 학습 완료된 PyTorch 모델(`.pt`)을 **TFLite(`.tflite`)** 포맷으로 변환. **INT8 양자화**를 적용하여 모델 크기를 약 75% 줄이고 추론 속도 향상
6.  **안드로이드 앱 통합**: CameraX로 실시간 카메라 프레임을 받아와 TFLite 모델로 추론하고, 그 결과를 커스텀 View를 통해 화면에 시각화

<br>

## 🚀 실행 방법

1.  본 GitHub 저장소를 `clone` 받습니다.
    ```bash
    git clone https://github.com/02hyeok/ecoeye.git
    ```
2.  Android Studio에서 프로젝트를 엽니다.
3.  `app/src/main/assets` 폴더에 학습된 TFLite 모델 파일(`best-int8.tflite`)이 있는지 확인합니다.
4.  안드로이드 기기 또는 에뮬레이터에서 앱을 빌드하고 실행합니다.

<br>

## 📈 향후 개선 과제

  * **분류 클래스 확장**: 플라스틱, 비닐 등 더 다양한 쓰레기 종류 추가
  * **정확도 향상**: 더 많은 데이터와 다양한 Augmentation 기법을 적용하여 모델 성능 고도화
  * **서버 연동**: 탐지 결과를 서버에 전송하여 공간별 분리수거 통계 데이터 수집 기능 추가
  * **iOS 버전 개발**: Swift와 Core ML을 사용하여 iOS 플랫폼으로 확장
