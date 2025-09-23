import torch
print(f"PyTorch version: {torch.__version__}")
# GPU가 없으므로 False가 출력되는 것이 정상입니다.
print(f"CUDA available: {torch.cuda.is_available()}")