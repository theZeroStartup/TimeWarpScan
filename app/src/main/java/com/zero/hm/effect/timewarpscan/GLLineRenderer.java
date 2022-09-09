package com.zero.hm.effect.timewarpscan;

import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.util.Log;
import android.view.Surface;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class GLLineRenderer {
    private final float[] mvpMatrix = new float[16];
    float color[] = { 0.0f, 0.0f, 0.0f, 1.0f };

    float lineCoords[] = new float[6];
    private int vertexCount;
    private int vertexStride;

    private int[] bos = new int[1];
    private int[] textures = new int[2];
    private int[] frameBuffers = new int[1];

    private SurfaceTexture surfaceTexture;
    private Surface surface;

    protected int glProgram = -1;
    protected int positionHandle;
    protected int colorHandle;
    protected int MVPMatrixHandle;

    private FloatBuffer vertexBuffer;

    // number of coordinates per vertex in this array
    private static final int COORDS_PER_VERTEX = 3;

    public void initShader() {
        final String vertexShaderCode =
                // This matrix member variable provides a hook to manipulate
                // the coordinates of the objects that use this vertex shader
                "uniform mat4 uMVPMatrix;" +
                        "attribute vec4 vPosition;" +
                        "void main() {" +
                        // the matrix must be included as a modifier of gl_Position
                        "  gl_Position = uMVPMatrix * vPosition;" +
                        "}";

        final String fragmentShaderCode =
                "precision mediump float;" +
                        "uniform vec4 vColor;" +
                        "void main() {" +
                        "  gl_FragColor = vColor;" +
                        "}";

        int vertexShader = ShaderUtils.loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = ShaderUtils.loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

        glProgram = ShaderUtils.createProgram(vertexShaderCode, fragmentShaderCode);             // create empty OpenGL ES Program
        GLES20.glAttachShader(glProgram, vertexShader);   // add the vertex shader to program
        GLES20.glAttachShader(glProgram, fragmentShader); // add the fragment shader to program
        GLES20.glLinkProgram(glProgram);                  // creates OpenGL ES program executables

        lineCoords = new float[] {
                0.0f, 0.0f, 0.0f,
                1.0f, 0.0f, 0.0f
        };

        // initialize vertex byte buffer for shape coordinates
        vertexBuffer = ByteBuffer.allocateDirect(
                // (number of coordinate values * 4 bytes per float)
                lineCoords.length * 4)
                .order(ByteOrder.nativeOrder()) // use the device hardware's native byte order
                .asFloatBuffer() // create a floating point buffer from the ByteBuffer
                .put(lineCoords); // add the coordinates to the FloatBuffer
        vertexBuffer.position(0); // set the buffer to read the first coordinate

        vertexCount = lineCoords.length / COORDS_PER_VERTEX;
        vertexStride = COORDS_PER_VERTEX * 4; // 4 bytes per vertex

        GLES20.glGenBuffers(1, bos, 0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, bos[0]);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, lineCoords.length * 4, vertexBuffer, GLES20.GL_STATIC_DRAW);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

        GLES20.glGenTextures(textures.length, textures, 0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textures[0]);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[1]);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

        GLES20.glGenFramebuffers(frameBuffers.length, frameBuffers,0);

        if(surfaceTexture != null){
            surfaceTexture.release();
        }
        surfaceTexture = new SurfaceTexture(textures[0]);
        if(surface != null){
            surface.release();
        }
        surface = new Surface(surfaceTexture);
    }

    public void setOnFrameAvailableListener(SurfaceTexture.OnFrameAvailableListener listener){
        if(surfaceTexture == null){
            return;
        }
        surfaceTexture.setOnFrameAvailableListener(listener);
    }
    public Surface getSurface() {
        if(surfaceTexture == null){
            return null;
        }
        return surface;
    }

    public void setVertices(float v0, float v1, float v2, float v3, float v4, float v5) {
        lineCoords[0] = v0;
        lineCoords[1] = v1;
        lineCoords[2] = v2;
        lineCoords[3] = v3;
        lineCoords[4] = v4;
        lineCoords[5] = v5;

        vertexBuffer.put(lineCoords);
//        // set the buffer to read the first coordinate
        vertexBuffer.position(0);
    }

    public void setColor(float red, float green, float blue, float alpha) {
        color[0] = red;
        color[1] = green;
        color[2] = blue;
        color[3] = alpha;
    }

    private int width,height;
    public void setSize(int width,int height){
        if(surfaceTexture == null){
            return;
        }
        this.width = width;
        this.height = height;
        surfaceTexture.setDefaultBufferSize(width,height);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBuffers[0]);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[1]);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, textures[1], 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }

    public void drawFrame() {

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBuffers[0]);
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glViewport(0,0,width,height);

        // Add program to OpenGL ES environment
        GLES20.glUseProgram(glProgram);

        // get handle to vertex shader's vPosition member
        positionHandle = GLES20.glGetAttribLocation(glProgram, "vPosition");

        // Enable a handle to the triangle vertices
        GLES20.glEnableVertexAttribArray(positionHandle);

        // Prepare the triangle coordinate data
        GLES20.glVertexAttribPointer(positionHandle, COORDS_PER_VERTEX,
                GLES20.GL_FLOAT, false,
                vertexStride, vertexBuffer);

        // get handle to fragment shader's vColor member
        colorHandle = GLES20.glGetUniformLocation(glProgram, "vColor");

        // Set color for drawing the triangle
        GLES20.glUniform4fv(colorHandle, 1, color, 0);

        // get handle to shape's transformation matrix
        MVPMatrixHandle = GLES20.glGetUniformLocation(glProgram, "uMVPMatrix");
        ShaderUtils.checkGlError("glGetUniformLocation");

        // Apply the projection and view transformation
        GLES20.glUniformMatrix4fv(MVPMatrixHandle, 1, false, mvpMatrix, 0);
        ShaderUtils.checkGlError("glUniformMatrix4fv");

        GLES20.glLineWidth(5f);
        // Draw the triangle
        GLES20.glDrawArrays(GLES20.GL_LINES, 0, vertexCount);

        // Disable vertex array
        GLES20.glDisableVertexAttribArray(positionHandle);
    }
}