/*
 * Author: <AUTHORNAME>
 * Committer: <COMMITTERNAME>
 *
 * Create Date:    Tue Sep 20 15:07:10 CEST 2016 
 * Module Name:    J1 - Toplevel CPU (Core, Memory)
 * Project Name:   J1Sc - A simple J1 implementation in Scala using Spinal HDL
 *
 * Hash: 0a82e2997654f501bf8937447a1521127602ea03
 * Date: Tue Nov 1 15:35:55 2016 +0100
 */
import spinal.core._
import spinal.lib._

class J1(cfg : J1Config) extends Component {

  // I/O ports
  val io = new Bundle {

    // Interface for the interrupt system
    val irq   = in Bool
    val intNo = in UInt(log2Up(cfg.irqConfig.numOfInterrupts) bits)

    // I/O signals for peripheral data port
    val cpuBus = master(SimpleBus(cfg))

  }.setName("")

  // Create a new CPU core
  val coreJ1CPU = new J1Core(cfg)

  // Create the main memory
  val mainMem = new MainMemory(cfg)

  // Instruction port (read only)
  mainMem.io.memInstrAdr <> coreJ1CPU.io.nextInstrAdr
  coreJ1CPU.io.memInstr <> mainMem.io.memInstr

  // Select from which source the data should be read
  val coreMemRead = coreJ1CPU.io.ioReadMode ? io.cpuBus.readData | mainMem.io.memRead

  // Connect the CPU core with the main memory (convert the byte address to a cell address)
  mainMem.io.memWriteEnable <> coreJ1CPU.io.memWriteMode
  mainMem.io.memAdr <> coreJ1CPU.io.extAdr(coreJ1CPU.io.extAdr.high downto 1)
  mainMem.io.memWrite <> coreJ1CPU.io.extToWrite

  // Read port of CPU core (multiplexed)
  coreJ1CPU.io.toRead <> coreMemRead

  // Connect the external bus to the core (remember coreJ1CPU.io.extAdr is one clock too early)
  io.cpuBus.enable    := coreJ1CPU.io.ioWriteMode || coreJ1CPU.io.ioReadMode
  io.cpuBus.writeMode <> coreJ1CPU.io.ioWriteMode
  io.cpuBus.address   <> Delay(coreJ1CPU.io.extAdr, 1)
  io.cpuBus.writeData <> coreJ1CPU.io.extToWrite

  // Connect the interrupts
  coreJ1CPU.io.intNo <> io.intNo
  coreJ1CPU.io.irq <> io.irq

}
