package org.sensingkit.flaas;

import org.sensingkit.flaaslib.utils.Utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.channels.FileChannel;

public class MLModel {

    @SuppressWarnings("unused")
    private static final String TAG = MLModel.class.getSimpleName();

    private static final int FLOAT_BYTES = 4;  // * 4 since 32 bit (4 bytes)

    private static FloatBuffer readModel(File file) throws IOException {

        byte[] data = Utils.loadData(file);
        ByteBuffer buffer = ByteBuffer.wrap(data);
        buffer.order(ByteOrder.nativeOrder());
        return buffer.asFloatBuffer();
    }

    private static void writeModel(ByteBuffer outputModel, File file) throws IOException {

        FileChannel channel = new FileOutputStream(file, false).getChannel();
        outputModel.rewind();
        channel.write(outputModel);
        channel.close();
    }

    public static void applyFedAvg(File[] inputFiles, int size, File outputFile) throws IOException {

        // init array of ByteBuffers
        FloatBuffer[] inputModels = new FloatBuffer[inputFiles.length];

        // read models
        for (int i = 0; i < inputModels.length; i++) {
            inputModels[i] = readModel(inputFiles[i]);

            // also check capacity
            if (inputModels[i].capacity() != size) {
                throw new RuntimeException("Models should have the same capacity of " + size +
                        " (modelParameters:" + inputModels[i].capacity() + ")");
            }
        }

        // init new model for output
        ByteBuffer outputModel = ByteBuffer.allocate(size * FLOAT_BYTES);
        outputModel.order(ByteOrder.nativeOrder());

        // merge models into output model
        for (int i = 0; i < size; i++) {
            //int bytePosition = i * FLOAT_BYTES;

            float value = 0;
            for (FloatBuffer inputModel : inputModels) {
                value += inputModel.get();
            }
            outputModel.putFloat(value / inputModels.length);
        }

        // write into file
        writeModel(outputModel, outputFile);
    }
}
