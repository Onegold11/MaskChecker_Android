package com.onegold.maskchecker;

import org.tensorflow.lite.Interpreter;

import java.nio.ByteBuffer;

public class TFLiteModel {
    public static final String MODEL_NAME = "MobileNet.tflite";
    private Interpreter interpreter; // 모델 객체

    /* 모델 설정 */
    public TFLiteModel(Interpreter interpreter) {
        this.interpreter = interpreter;
    }

    /* 모델 실행 */
    public float[][] run(ByteBuffer input){
        float[][] output = new float[1][2];

        if (interpreter != null)
            interpreter.run(input, output);

        return output;
    }

    public Interpreter getInterpreter() {
        return interpreter;
    }

    public void setInterpreter(Interpreter interpreter) {
        this.interpreter = interpreter;
    }
}
