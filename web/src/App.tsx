import './App.css'
import React, { useEffect, useRef, useState } from 'react';
import * as tf from '@tensorflow/tfjs';

const CLASSES = ['cans_bottles', 'general_waste', 'paper'];
const SCORE_THRESHOLD = 0.5;

function App() {
  const videoRef = React.useRef<HTMLVideoElement>(null);
  const canvasRef = React.useRef<HTMLCanvasElement>(null);

  const modelRef = useRef<tf.GraphModel | null>(null);

  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    // 추론 루프 함수
    const predictLoop = async () => {
      if (modelRef.current && videoRef.current && canvasRef.current && videoRef.current.readyState >= 3) {
        const model = modelRef.current;
        const video = videoRef.current;

        // console.log("추론 시작...");

        // 메모리 누수 방지 위한 tf.tidy()
        try {
          let boxes, scores, classes;

          tf.tidy(() => {
            // 전처리: 비디오 프레임을 텐서로 변환
            const videoTensor = tf.browser.fromPixels(video);
            const resizedTensor = tf.image.resizeBilinear(videoTensor, [320, 320]);
            const inputTensor = resizedTensor.div(255.0).expandDims(0);

            // 모델 추론 실행
            const outputTensor = model.execute(inputTensor) as tf.Tensor;

            // 결과 후처리
            const results = postProcessSync(outputTensor);
            boxes = results[0];
            scores = results[1];
            classes = results[2];
          });

          // 그리기
          if (boxes && scores && classes) {
            drawResults(boxes, scores, classes);
          }
        } catch (err) {
          console.error("추론 루프 중 오류:", err);
        }
      }
      // 다음 프레임에서 반복적으로 호출
      requestAnimationFrame(predictLoop);
    };

    async function setup() {
      try {
        // TF.js 백엔드 설정 (WebGL 우선)
        await tf.setBackend('webgl');
        await tf.ready();

        // 모델 로드
        // console.log("모델 로딩 시작...");
        modelRef.current = await tf.loadGraphModel('/model/best_web_model/model.json');
        // console.log("모델 로드 성공.");
        setIsLoading(false);

        // 카메라 활성화 (getUserMedia)
        // console.log("카메라 설정 시작...");
        const stream = await navigator.mediaDevices.getUserMedia({
          video: { facingMode: 'environment' },
          audio: false,
        });

        if (videoRef.current) {
          videoRef.current.srcObject = stream;
          videoRef.current.onloadedmetadata = () => {
            // console.log("비디오 메타데이터 로드됨.");
            videoRef.current?.play().catch(err => {
              console.error("video.play() 호출 실패:", err);
            });
          };
          videoRef.current.onplaying = () => {
            // console.log(`카메라 재생 시작. (현재 readyState: ${videoRef.current?.readyState})`);
          };
        }
      } catch (err) {
        console.error("초기화 실패:", err);
        setIsLoading(false);
      }
    }
    setup();
    predictLoop();
  }, []);

  // YOLOv8 후처리 함수
  const postProcessSync = (outputTensor: tf.Tensor) => {
    let boxesTensor, scoresTensor, maxScores, classIndices, nmsIndicesTensor;
    let finalBoxesTensor, finalScoresTensor, finalClassesTensor;

    try {
      // [1, 7, 8400] -> [8400, 7]로 변환
      const transposed = outputTensor.squeeze().transpose(); // [8400, 7]
      
      // 박스 좌표 (cx, cy, w, h)
      const boxesTensor = transposed.slice([0, 0], [-1, 4]);
      // 클래스 점수 (cls1, cls2, cls3)
      const scoresTensor = transposed.slice([0, 4], [-1, 3]);

      // 각 박스에서 가장 높은 클래스 점수와 해당 인덱스(클래스 ID) 찾기
      const maxScores = scoresTensor.max(1) as tf.Tensor1D; // 8400개의 최대 점수
      const classIndices = scoresTensor.argMax(1) as tf.Tensor1D; // 8400개의 클래스 ID

      // Non-Max Suppression (NMS) 실행
      // 점수가 SCORE_THRESHOLD 이상인 박스들만 NMS 대상으로 함
      const nmsIndicesTensor = tf.image.nonMaxSuppression(
        boxesTensor as tf.Tensor2D, // 박스 좌표
        maxScores,                  // 박스 점수
        100,                        // 최대 박스 개수
        0.45,                       // IoU 임계값
        SCORE_THRESHOLD             // 점수 임계값
      );

      // NMS 결과(인덱스)를 CPU로 가져와 동기 처리
      const nmsIndices = nmsIndicesTensor.dataSync();

      // 최종 결과 텐서 생성
      finalBoxesTensor = tf.gather(boxesTensor, nmsIndices);
      finalScoresTensor = tf.gather(maxScores, nmsIndices);
      finalClassesTensor = tf.gather(classIndices, nmsIndices);

      // 실제 데이터(JS 배열) 추출
      const finalBoxes = finalBoxesTensor.dataSync() as Float32Array;
      const finalScores = finalScoresTensor.dataSync() as Float32Array;
      const finalClasses = finalClassesTensor.dataSync() as Float32Array;
      
      return [finalBoxes, finalScores, finalClasses];
    } finally {
      tf.dispose([nmsIndicesTensor, boxesTensor, scoresTensor, maxScores, classIndices, outputTensor]);
    }
  };

  // 캔버스에 그리기 함수
  const drawResults = (boxes: Float32Array, scores: Float32Array, classes: Float32Array) => {
    const canvas = canvasRef.current;
    const video = videoRef.current;
    if (!canvas || !video) return;

    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    // 캔버스 크기를 비디오 원본 크기와 맞추기
    const videoWidth = video.videoWidth;
    const videoHeight = video.videoHeight;
    canvas.width = videoWidth;
    canvas.height = videoHeight;

    // 캔버스 지우기
    ctx.clearRect(0, 0, ctx.canvas.width, ctx.canvas.height);

    // NMS 결과를 순회하며 그리기
    for (let i = 0; i < scores.length; ++i) {
      if (scores[i] < SCORE_THRESHOLD) continue;

      // 박스 좌표 (cx, cy, w, h) -> (x1, y1, w, h)로 변환
      // 및 좌표 스케일링 (320 -> 원본 비디오 크기)
      const [cx_norm, cy_norm, w_norm, h_norm] = boxes.slice(i * 4, (i + 1) * 4);
      const centerX = cx_norm;
      const centerY = cy_norm;
      const widthPixel = w_norm;
      const heightPixel = h_norm;
      const x1_raw = centerX - widthPixel / 2;
      const y1_raw = centerY - heightPixel / 2;

      const scaleX = videoWidth / 320;
      const scaleY = videoHeight / 320;
      
      const x1 = x1_raw * scaleX;
      const y1 = y1_raw * scaleY;
      const scaledWidth = widthPixel * scaleX;
      const scaledHeight = heightPixel * scaleY;

      // 클래스 이름과 점수
      const label = `${CLASSES[classes[i]]} ${Math.round(scores[i] * 100)}%`;
      
      // UI 그리기
      const color = "green";
      ctx.strokeStyle = color;
      ctx.lineWidth = 4;
      ctx.strokeRect(x1, y1, scaledWidth, scaledHeight);

      // 라벨 배경 그리기
      ctx.fillStyle = color;
      const textWidth = ctx.measureText(label).width;
      ctx.fillRect(x1 - 2, y1 - 20, textWidth + 4, 20);

      // 라벨 텍스트 그리기
      ctx.fillStyle = "white";
      ctx.font = '16px Arial';
      ctx.fillText(label, x1, y1 - 5);
    }
  };

  return (
    <div className="app-container"> 
      
      <header className="app-header">
        <h1>♻️ EcoEye Trash Detector</h1>
      </header>

      <main className="camera-area">
        {isLoading && (
          <div className="loading-overlay">
            <p>AI 모델을 불러오는 중입니다...</p>
          </div>
        )}
        
        <video
          ref={videoRef}
          className="video-feed" 
          autoPlay
          playsInline
          muted
        />
        <canvas
          ref={canvasRef}
          className="overlay-canvas" 
        />
      </main>

      <footer className="app-footer">
        <p>Point your camera at the trash</p>
      </footer>
    </div>
  );
}

export default App
