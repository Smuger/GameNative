package com.winlator.xserver.requests;

import static com.winlator.xserver.Keyboard.KEYSYMS_PER_KEYCODE;
import static com.winlator.xserver.XClientRequestHandler.RESPONSE_CODE_SUCCESS;

import com.winlator.xconnector.XInputStream;
import com.winlator.xconnector.XOutputStream;
import com.winlator.xconnector.XStreamLock;
import com.winlator.xserver.Keyboard;
import com.winlator.xserver.XClient;
import com.winlator.xserver.errors.XRequestError;

import java.io.IOException;

public abstract class KeyboardRequests {
    public static void getKeyboardMapping(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException, XRequestError {
        byte firstKeycode = inputStream.readByte();
        int count = inputStream.readUnsignedByte();
        inputStream.skip(2);

        int totalKeysyms = count * KEYSYMS_PER_KEYCODE;

        try (XStreamLock lock = outputStream.lock()) {
            outputStream.writeByte(RESPONSE_CODE_SUCCESS);
            outputStream.writeByte(KEYSYMS_PER_KEYCODE);
            outputStream.writeShort(client.getSequenceNumber());
            outputStream.writeInt(totalKeysyms);
            outputStream.writePad(24);

            int baseIndex = (firstKeycode - Keyboard.MIN_KEYCODE) * KEYSYMS_PER_KEYCODE;
            for (int i = 0; i < totalKeysyms; i++) {
                int idx = baseIndex + i;
                if (idx >= 0 && idx < client.xServer.keyboard.keysyms.length) {
                    outputStream.writeInt(client.xServer.keyboard.keysyms[idx]);
                } else {
                    outputStream.writeInt(0);
                }
            }
        }
    }

    public static void getModifierMapping(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException, XRequestError {
        try (XStreamLock lock = outputStream.lock()) {
            outputStream.writeByte(RESPONSE_CODE_SUCCESS);
            outputStream.writeByte((byte)1);
            outputStream.writeShort(client.getSequenceNumber());
            outputStream.writeInt(2);
            outputStream.writePad(24);
            outputStream.writePad(8);
        }
    }
}