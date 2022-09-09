package com.zero.hm.effect.timewarpscan;

import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;

import android.content.Context;
import android.content.SharedPreferences;
import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Objects;

public class GLScanRenderer {
    private int programId = -1;
    private int aPositionHandle;
    private int sTextureSamplerHandle;
    private int uTextureSamplerHandle;
    private int aTextureCoordHandle;
    private int scanHeightHandle;

    private int[] bos = new int[2];
    private int[] textures = new int[2];
    private int[] frameBuffers = new int[2];

    public void initShader() {
        float[] vertexData = {
                1f, -1f, 0f,
                -1f, -1f, 0f,
                1f, 1f, 0f,
                -1f, 1f, 0f
        };

        float[] textureVertexData = {
                1f, 0f,
                0f, 0f,
                1f, 1f,
                0f, 1f
        };
        FloatBuffer vertexBuffer = ByteBuffer.allocateDirect(vertexData.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(vertexData);
        vertexBuffer.position(0);

        FloatBuffer textureVertexBuffer = ByteBuffer.allocateDirect(textureVertexData.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(textureVertexData);
        textureVertexBuffer.position(0);

        GLES20.glGenBuffers(2, bos, 0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, bos[0]);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, vertexData.length * 4, vertexBuffer, GLES20.GL_STATIC_DRAW);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, bos[1]);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, textureVertexData.length * 4, textureVertexBuffer, GLES20.GL_STATIC_DRAW);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

        GLES20.glGenTextures(textures.length, textures, 0);
        for (int texture : textures) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        }

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

        GLES20.glGenFramebuffers(frameBuffers.length, frameBuffers,0);
    }

    private String getWarpMode(Context context) {
        SharedPreferences sharedPref = context.getSharedPreferences("camera", Context.MODE_PRIVATE);
        return sharedPref.getString("warpMode", "horizontal");
    }

    private int width,height;
    public void setSize(int width,int height){
        this.width = width;
        this.height = height;
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBuffers[0]);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, textures[0], 0);

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBuffers[1]);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[1]);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, textures[1], 0);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }
    private int textureIndex = 0;
    public void drawFrame(int texture, float scanHeight, Context context, boolean isNewScan) {
        int index = textureIndex;
        textureIndex = (index+1)%2;

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBuffers[textureIndex]);
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GL_COLOR_BUFFER_BIT);
        GLES20.glViewport(0,0,width,height);
        GLES20.glUseProgram(getProgramId(context, isNewScan));
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,texture);
        GLES20.glUniform1i(sTextureSamplerHandle,0);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,textures[index]);
        GLES20.glUniform1i(uTextureSamplerHandle,1);

        GLES20.glUniform1f(scanHeightHandle,scanHeight);

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, bos[0]);
        GLES20.glEnableVertexAttribArray(aPositionHandle);
        GLES20.glVertexAttribPointer(aPositionHandle, 3, GLES20.GL_FLOAT, false,
                0, 0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, bos[1]);
        GLES20.glEnableVertexAttribArray(aTextureCoordHandle);
        GLES20.glVertexAttribPointer(aTextureCoordHandle, 2, GLES20.GL_FLOAT, false, 0, 0);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);

        GLES20.glClear(GL_COLOR_BUFFER_BIT);
    }

    private int getProgramId(Context context, boolean isNewScan) {
        if (programId == -1 || isNewScan) {
            String fragmentShader;

            if (Objects.equals(getWarpMode(context), "horizontal")) {
                fragmentShader = "varying highp vec2 vTexCoord;\n" +
                        "uniform sampler2D sTexture;\n" +
                        "uniform sampler2D uTexture;\n" +
                        "uniform highp float scanHeight;\n" +
                        "void main() {\n" +
                        "   highp float fy = vTexCoord.x;\n" +
                        "   if(fy > scanHeight){" +
                        "       highp vec4 rgba = texture2D(sTexture , vTexCoord);\n" +
                        "       gl_FragColor = rgba;\n" +
                        "   }else{" +
                        "       highp vec4 rgba = texture2D(uTexture , vTexCoord);\n" +
                        "       gl_FragColor = rgba;\n" +
                        "   }\n" +
                        "}";
            } else {
                fragmentShader = "varying highp vec2 vTexCoord;\n" +
                        "uniform sampler2D sTexture;\n" +
                        "uniform sampler2D uTexture;\n" +
                        "uniform highp float scanHeight;\n" +
                        "void main() {\n" +
                        "   highp float fy = 1.0 - vTexCoord.y;\n" +
                        "   if(fy > scanHeight){" +
                        "       highp vec4 rgba = texture2D(sTexture, vTexCoord);\n" +
                        "       gl_FragColor = rgba;\n" +
                        "   }else{" +
                        "       highp vec4 rgba = texture2D(uTexture , vTexCoord);\n" +
                        "       gl_FragColor = rgba;\n" +
                        "   }\n" +
                        "}";
            }

            String vertexShader = "attribute vec4 aPosition;\n" +
                    "attribute vec2 aTexCoord;\n" +
                    "varying vec2 vTexCoord;\n" +
                    "void main() {\n" +
                    "  vTexCoord = aTexCoord;\n" +
                    "  gl_Position = aPosition;\n" +
                    "}";
            programId = ShaderUtils.createProgram(vertexShader, fragmentShader);
            aPositionHandle = GLES20.glGetAttribLocation(programId, "aPosition");
            sTextureSamplerHandle = GLES20.glGetUniformLocation(programId, "sTexture");
            uTextureSamplerHandle = GLES20.glGetUniformLocation(programId, "uTexture");
            aTextureCoordHandle = GLES20.glGetAttribLocation(programId, "aTexCoord");
            scanHeightHandle = GLES20.glGetUniformLocation(programId, "scanHeight");
        }

        return programId;
    }

    public int getTexture() {
        return textures[textureIndex];
    }

    public void release() {
        GLES20.glDeleteProgram(programId);
        GLES20.glDeleteFramebuffers(frameBuffers.length,frameBuffers,0);
        GLES20.glDeleteTextures(textures.length, textures, 0);
        GLES20.glDeleteBuffers(bos.length, bos, 0);
    }
}
