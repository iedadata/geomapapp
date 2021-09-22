package haxby.image.jcodec.containers.mps;
import static haxby.image.jcodec.common.Preconditions.checkState;

import java.io.File;
import java.io.IOException;
import java.lang.System;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

import haxby.image.jcodec.common.IntIntMap;
import haxby.image.jcodec.common.io.NIOUtils;
import haxby.image.jcodec.common.tools.MainUtils;
import haxby.image.jcodec.common.tools.MainUtils.Cmd;
import haxby.image.jcodec.containers.mps.psi.PATSection;
import haxby.image.jcodec.containers.mps.psi.PMTSection;
import haxby.image.jcodec.containers.mps.psi.PMTSection.PMTStream;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 */
public class MTSPktDump {

    public static void main1(String[] args) throws IOException {

        Cmd cmd = MainUtils.parseArguments(args, new MainUtils.Flag[] {});
        if (cmd.args.length < 1) {
            MainUtils.printHelpNoFlags("file name");
            return;
        }

        ReadableByteChannel ch = null;
        try {
            ch = NIOUtils.readableChannel(new File(cmd.args[0]));
            dumpTSPackets(ch);
        } finally {
            NIOUtils.closeQuietly(ch);
        }
    }

    private static void dumpTSPackets(ReadableByteChannel _in) throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(188 * 1024);

        while (_in.read(buf) != -1) {
            buf.flip();
            buf.limit((buf.limit() / 188) * 188);
            int pmtPid = -1;
            for (int pkt = 0; buf.hasRemaining(); ++pkt) {
                ByteBuffer tsBuf = NIOUtils.read(buf, 188);
                checkState(0x47 == (tsBuf.get() & 0xff));
                int guidFlags = ((tsBuf.get() & 0xff) << 8) | (tsBuf.get() & 0xff);
                int guid = (int) guidFlags & 0x1fff;
                int payloadStart = (guidFlags >> 14) & 0x1;
                int b0 = tsBuf.get() & 0xff;
                int counter = b0 & 0xf;
                if ((b0 & 0x20) != 0) {
                    NIOUtils.skip(tsBuf, (tsBuf.get() & 0xff));
                }
                System.out.print("#" + pkt + "[guid: " + guid + ", cnt: " + counter + ", start: "
                        + (payloadStart == 1 ? "y" : "-"));

                if (guid == 0 || guid == pmtPid) {

                    System.out.print(", PSI]: ");
                    if (payloadStart == 1) {
                        NIOUtils.skip(tsBuf, (tsBuf.get() & 0xff));
                    }

                    if (guid == 0) {
                        PATSection pat = PATSection.parsePAT(tsBuf);
                        IntIntMap programs = pat.getPrograms();
                        pmtPid = programs.values()[0];
                        printPat(pat);
                    } else if (guid == pmtPid) {
                        PMTSection pmt = PMTSection.parsePMT(tsBuf);
                        printPmt(pmt);
                    }
                } else {
                    System.out.print("]: " + tsBuf.remaining());
                }
                System.out.println();
            }
            buf.clear();
        }
    }

    private static void printPat(PATSection pat) {
        IntIntMap programs = pat.getPrograms();
        System.out.print("PAT: ");
        int[] keys = programs.keys();
        for (int i : keys) {
            System.out.print(i + ":" + programs.get(i) + ", ");
        }
    }

    private static void printPmt(PMTSection pmt) {
        System.out.print("PMT: ");
        for (PMTStream pmtStream : pmt.getStreams()) {
            System.out.print(pmtStream.getPid() + ":" + pmtStream.getStreamTypeTag() + ", ");
        }
    }

}
