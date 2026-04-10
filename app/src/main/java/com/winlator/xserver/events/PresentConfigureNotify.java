package com.winlator.xserver.events;

import com.winlator.xconnector.XOutputStream;
import com.winlator.xconnector.XStreamLock;
import com.winlator.xserver.Window;
import com.winlator.xserver.extensions.PresentExtension;

import java.io.IOException;

public class PresentConfigureNotify extends Event {
    private final int eventId;
    private final Window window;
    private final short x;
    private final short y;
    private final short width;
    private final short height;

    public PresentConfigureNotify(int eventId, Window window, int x, int y, int width, int height) {
        super(35);
        this.eventId = eventId;
        this.window = window;
        this.x = (short)x;
        this.y = (short)y;
        this.width = (short)width;
        this.height = (short)height;
    }

    @Override
    public void send(short sequenceNumber, XOutputStream outputStream) throws IOException {
        try (XStreamLock lock = outputStream.lock()) {
            outputStream.writeByte(code);
            outputStream.writeByte(PresentExtension.MAJOR_OPCODE);
            outputStream.writeShort(sequenceNumber);
            outputStream.writeInt(4);       // length in 4-byte units after header
            outputStream.writeShort(getEventType());
            outputStream.writePad(2);       // pad
            outputStream.writeInt(eventId);
            outputStream.writeInt(window.id);
            outputStream.writeShort(x);
            outputStream.writeShort(y);
            outputStream.writeShort(width);
            outputStream.writeShort(height);
            outputStream.writeShort((short)0); // off_x
            outputStream.writeShort((short)0); // off_y
            outputStream.writeShort(width);    // pixmap_width
            outputStream.writeShort(height);   // pixmap_height
            outputStream.writeInt(0);          // pixmap_flags
        }
    }

    public static short getEventType() {
        return 0;
    }

    public static int getEventMask() {
        return 1 << getEventType();
    }
}
