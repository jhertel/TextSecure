/**
 * Copyright (C) 2013 Open Whisper Systems
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.whispersystems.textsecure.push;

import android.util.Log;

import org.whispersystems.textsecure.crypto.TransportDetails;

import java.io.IOException;

public class PushTransportDetails implements TransportDetails {

  private final int messageVersion;

  public PushTransportDetails(int messageVersion) {
    this.messageVersion = messageVersion;
  }

  @Override
  public byte[] getStrippedPaddingMessageBody(byte[] messageWithPadding) {
    if       (messageVersion < 2) throw new AssertionError("Unknown version: " + messageVersion);
    else if (messageVersion == 2) return messageWithPadding;

    int paddingStart = 0;

    for (int i=messageWithPadding.length-1;i>=0;i--) {
      if (messageWithPadding[i] == (byte)0x80) {
        paddingStart = i;
        break;
      } else if (messageWithPadding[i] != (byte)0x00) {
        Log.w("PushTransportDetails", "Padding byte is malformed, returning unstripped padding.");
        return messageWithPadding;
      }
    }

    byte[] strippedMessage = new byte[messageWithPadding.length - paddingStart];
    System.arraycopy(messageWithPadding, 0, strippedMessage, 0, strippedMessage.length);

    return strippedMessage;
  }

  @Override
  public byte[] getPaddedMessageBody(byte[] messageBody) {
    if       (messageVersion < 2) throw new AssertionError("Unknown version: " + messageVersion);
    else if (messageVersion == 2) return messageBody;

    byte[] paddedMessage = new byte[getPaddedMessageLength(messageBody.length)];
    System.arraycopy(messageBody, 0, paddedMessage, 0, messageBody.length);
    paddedMessage[messageBody.length] = (byte)0x80;

    return paddedMessage;
  }

  @Override
  public byte[] getEncodedMessage(byte[] messageWithMac) {
    return messageWithMac;
  }

  @Override
  public byte[] getDecodedMessage(byte[] encodedMessageBytes) throws IOException {
    return encodedMessageBytes;
  }

  private int getPaddedMessageLength(int messageLength) {
    int messageLengthWithTerminator = messageLength + 1;
    int messagePartCount            = messageLengthWithTerminator / 160;

    if (messageLengthWithTerminator % 160 != 0) {
      messagePartCount++;
    }

    return messagePartCount * 160;
  }
}
