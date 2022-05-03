package org.sensingkit.flaaslib.dataset.cifar;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;


public class CIFAR10BatchFileParser {

    public static final int IMAGE_WIDTH = 32;
    public static final int IMAGE_HEIGHT = 32;
    public static final int IMAGE_TOTAL = IMAGE_WIDTH * IMAGE_HEIGHT;

    public static final int IMAGE_BLOCK_SIZE = IMAGE_TOTAL * 3;
    public static final int TOTAL_BLOCK_SIZE = 1 + IMAGE_BLOCK_SIZE;

    private static final int LOWER_BYTE_MASK = 0xFF;

    private static final String[] classes = {"airplane", "automobile", "bird", "cat", "deer", "dog", "frog", "horse", "ship", "truck"};

    private final FileInputStream inputStream;
    private byte[] data;
//    private ImageProcessor imageProcessor;
    int imageSize;


    // --- Static Methods ---

    public static String[] getClasses() {
        return classes;
    }

    public static String getClass(int index) {
        return classes[index];
    }


    // --- Constructor ---

    public CIFAR10BatchFileParser(File sampleFile, int offset, int imageSize) throws IOException {

        this.imageSize = imageSize;

//        this.imageProcessor =
//                new ImageProcessor.Builder()
//                        .add(new ResizeOp(size, size, ResizeOp.ResizeMethod.BILINEAR))
////                        .add(new NormalizeOp(0, 255))
//                        .build();

        this.inputStream = new FileInputStream(sampleFile);

        if (offset > 0) {
            this.inputStream.skip((long) offset * TOTAL_BLOCK_SIZE);
        }
    }


    // --- Iterator (kind of) ---

    public boolean hasNext() {
        try {
            return inputStream.available() != 0;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void next() {

        // read new section
        this.data = new byte[TOTAL_BLOCK_SIZE];
        int size;
        try {
            size = inputStream.read(this.data);
        } catch (IOException e) {
            throw new RuntimeException("You reached the end of the file.");
        }

        if (size == -1) {  // all ok, just reached end of file
            return;
        }

        if (size != TOTAL_BLOCK_SIZE) {
            throw new RuntimeException("Something is wrong, size is not equal to BLOCK_SIZE");
        }
    }


    // --- Data Accessors ---

    public byte[] getBinaryData() {
        return data;
    }

    public int getLabel() {
        return this.data[0];
    }

    public float[] getOriginalData() {

        float[] floatValues = new float[IMAGE_BLOCK_SIZE];

        int nextIdx = 0;
        for (int x = 0; x < IMAGE_HEIGHT; x++) {
            for (int y = 0; y < IMAGE_WIDTH; y++) {

                float r = (this.data[1 + y * IMAGE_HEIGHT + x] & LOWER_BYTE_MASK) * (1 / 255.f);                    // 1 + IMAGE_SIZE * 0 + y * IMAGE_HEIGHT + x
                float g = (this.data[1 + IMAGE_TOTAL + y * IMAGE_HEIGHT + x] & LOWER_BYTE_MASK) * (1 / 255.f);      // 1 + IMAGE_SIZE * 1 + y * IMAGE_HEIGHT + x
                float b = (this.data[1 + IMAGE_TOTAL * 2 + y * IMAGE_HEIGHT + x] & LOWER_BYTE_MASK) * (1 / 255.f);  // 1 + IMAGE_SIZE * 2 + y * IMAGE_HEIGHT + x

                floatValues[nextIdx++] = r;
                floatValues[nextIdx++] = g;
                floatValues[nextIdx++] = b;
            }
        }

        return floatValues;
    }

    public float[] getData(boolean flipped) {

        Bitmap scaledBitmap = Bitmap.createScaledBitmap(
                getBitmap(flipped), 224, 224, true);

//        TensorImage tImage = new TensorImage(DataType.UINT8);
//        tImage.load(getBitmap(flipped));
//        Bitmap processedImage = imageProcessor.process(tImage).getBitmap();

        // hack to mimic Android example. We could use: imageProcessor.process(tImage).getTensorBuffer().getFloatArray()
        float[] normalizedRgb = new float[this.imageSize * this.imageSize * 3];
        int nextIdx = 0;
        for (int y = 0; y < this.imageSize; y++) {
            for (int x = 0; x < this.imageSize; x++) {
                int rgb = scaledBitmap.getPixel(x, y);

                float r = ((rgb >> 16) & LOWER_BYTE_MASK) * (1 / 255.f);
                float g = ((rgb >> 8) & LOWER_BYTE_MASK) * (1 / 255.f);
                float b = (rgb & LOWER_BYTE_MASK) * (1 / 255.f);

                normalizedRgb[nextIdx++] = r;
                normalizedRgb[nextIdx++] = g;
                normalizedRgb[nextIdx++] = b;
            }
        }

        return normalizedRgb;
    }

    public Bitmap getBitmap(boolean flipped) {

        Bitmap bitmap = Bitmap.createBitmap(IMAGE_WIDTH, IMAGE_HEIGHT, Bitmap.Config.ARGB_8888);

        for (int x = 0; x < IMAGE_HEIGHT; x++) {
            for (int y = 0; y < IMAGE_WIDTH; y++) {

                float r = (this.data[1 + y * IMAGE_HEIGHT + x] & LOWER_BYTE_MASK) * (1 / 255.f);                    // 1 + IMAGE_SIZE * 0 + y * IMAGE_HEIGHT + x
                float g = (this.data[1 + IMAGE_TOTAL + y * IMAGE_HEIGHT + x] & LOWER_BYTE_MASK) * (1 / 255.f);      // 1 + IMAGE_SIZE * 1 + y * IMAGE_HEIGHT + x
                float b = (this.data[1 + IMAGE_TOTAL * 2 + y * IMAGE_HEIGHT + x] & LOWER_BYTE_MASK) * (1 / 255.f);  // 1 + IMAGE_SIZE * 2 + y * IMAGE_HEIGHT + x

                bitmap.setPixel(x, y, Color.rgb(r, g, b));
            }
        }

        if (flipped) {
            Matrix matrix = new Matrix();
            matrix.preScale(-1.0f, 1.0f);
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        }

        return bitmap;
    }

    public void close() {
        try {
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
