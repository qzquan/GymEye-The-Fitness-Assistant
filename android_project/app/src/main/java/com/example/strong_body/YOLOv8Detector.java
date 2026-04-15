package com.example.strong_body;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.util.Log;

import org.tensorflow.lite.Interpreter;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

public class YOLOv8Detector {
    private Interpreter interpreter;
    private List<String> labels;

    private static final int INPUT_SIZE = 640;
    private static final int NUM_CLASSES = 4;
    private static final int NUM_ELEMENTS = 4 + NUM_CLASSES;
    private static final int NUM_BOXES = 8400;

    public YOLOv8Detector(Context context, String modelPath, String labelPath) {
        try {
            // 1. 原生加载模型文件 (摆脱 FileUtil)
            AssetFileDescriptor fileDescriptor = context.getAssets().openFd(modelPath);
            FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
            FileChannel fileChannel = inputStream.getChannel();
            MappedByteBuffer modelFile = fileChannel.map(FileChannel.MapMode.READ_ONLY, fileDescriptor.getStartOffset(), fileDescriptor.getDeclaredLength());

            Interpreter.Options options = new Interpreter.Options();
            options.setNumThreads(4);
            interpreter = new Interpreter(modelFile, options);

            // 2. 加载中文字典
            labels = new ArrayList<>();
            BufferedReader reader = new BufferedReader(new InputStreamReader(context.getAssets().open(labelPath)));
            String line;
            while ((line = reader.readLine()) != null) labels.add(line);
            reader.close();

        } catch (Exception e) {
            Log.e("YOLO", "模型加载失败", e);
        }
    }

    public String detect(Bitmap bitmap) {
        if (interpreter == null) return "模型未初始化";

        // 3. 原生图像预处理 (摆脱 ImageProcessor)
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1 * INPUT_SIZE * INPUT_SIZE * 3 * 4);
        byteBuffer.order(ByteOrder.nativeOrder());

        // 缩放图片到 640x640
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true);
        int[] intValues = new int[INPUT_SIZE * INPUT_SIZE];
        scaledBitmap.getPixels(intValues, 0, scaledBitmap.getWidth(), 0, 0, scaledBitmap.getWidth(), scaledBitmap.getHeight());

        // 提取像素并归一化 (0~1)
        int pixel = 0;
        for (int i = 0; i < INPUT_SIZE; ++i) {
            for (int j = 0; j < INPUT_SIZE; ++j) {
                final int val = intValues[pixel++];
                byteBuffer.putFloat(((val >> 16) & 0xFF) / 255.0f); // R
                byteBuffer.putFloat(((val >> 8) & 0xFF) / 255.0f);  // G
                byteBuffer.putFloat((val & 0xFF) / 255.0f);         // B
            }
        }

        // 4. 跑模型获取结果
        float[][][] output = new float[1][NUM_ELEMENTS][NUM_BOXES];
        interpreter.run(byteBuffer, output);

        String bestResult = "正在识别...";
        float highestConf = 0.5f;

        for (int i = 0; i < NUM_BOXES; i++) {
            float maxClassScore = 0;
            int classIndex = -1;
            for (int c = 0; c < NUM_CLASSES; c++) {
                float score = output[0][4 + c][i];
                if (score > maxClassScore) {
                    maxClassScore = score;
                    classIndex = c;
                }
            }
            if (maxClassScore > highestConf && classIndex != -1) {
                highestConf = maxClassScore;
                bestResult = labels.get(classIndex) + " (" + (int)(maxClassScore * 100) + "%)";
            }
        }
        return bestResult;
    }
}