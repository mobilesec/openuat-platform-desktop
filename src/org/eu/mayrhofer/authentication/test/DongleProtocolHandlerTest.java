package org.eu.mayrhofer.authentication.test;

import org.eu.mayrhofer.authentication.DongleProtocolHandler;

import junit.framework.*;

public class DongleProtocolHandlerTest extends TestCase {
	public void testAddPart() {
		byte[] dest = new byte[8];
		byte[] src1 = {0x01, 0x02, 0x03};
		byte[] src2 = {(byte) 0xff};
		byte[] src3 = {(byte) 0xfe};
		byte[] src4 = {(byte) 0xaa};
		
		DongleProtocolHandler.addPart(dest, src1, 0, 1);
		Assert.assertEquals(0x01, dest[0]);
		DongleProtocolHandler.addPart(dest, src1, 0, 8);
		Assert.assertEquals(0x01, dest[0]);
		DongleProtocolHandler.addPart(dest, src1, 8, 9);
		Assert.assertEquals(0x01, dest[0]);
		Assert.assertEquals(0x01, dest[1]);
		Assert.assertEquals(0x01, dest[2]);
		DongleProtocolHandler.addPart(dest, src1, 12, 6);
		Assert.assertEquals(0x01, dest[0]);
		Assert.assertEquals(0x11, dest[1]);
		DongleProtocolHandler.addPart(dest, src1, 0, 24);
		Assert.assertEquals(0x01, dest[0]);
		Assert.assertEquals(0x02, dest[1]);
		Assert.assertEquals(0x03, dest[2]);

		DongleProtocolHandler.addPart(dest, src2, 16, 4);
		Assert.assertEquals(0x0f, dest[2]);
		DongleProtocolHandler.addPart(dest, src2, 16, 8);
		Assert.assertEquals(0xff, dest[2]);

		DongleProtocolHandler.addPart(dest, src3, 0, 8);
		Assert.assertEquals(0xff, dest[0]);

		DongleProtocolHandler.addPart(dest, src4, 28, 8);
		Assert.assertEquals(0xa0, dest[3]);
		Assert.assertEquals(0x0a, dest[4]);

		DongleProtocolHandler.addPart(dest, src2, 34, 7);
		Assert.assertEquals(0xfc, dest[4]);
		Assert.assertEquals(0x01, dest[5]);
	}
}