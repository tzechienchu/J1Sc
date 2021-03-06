/*
 * Author: Steffen Reith (Steffen.Reith@hs-rm.de)
 *
 * Creation Date:  Sun Jan 22 12:51:03 GMT+1 2017
 * Module Name:    MainMemory - implementation of 64k words main memory
 * Project Name:   J1Sc - A simple J1 implementation in Scala using Spinal HDL
 *
 * Note: This module takes "small" blocks of BRAM and uses Spinal to tie them
 * together. The reason for doing so is the incapability of Xilinx Vivado 2016.4
 * the generate a dual ported BRAM bigger than 64K (one port one gives 0 as
 * a value back). Warning: The simulation works, but the real hardware causes
 * pain
 *
 */
import spinal.core._

class MainMemory(cfg : J1Config) extends Component {

  // Check the generic parameters
  assert(Bool(cfg.wordSize >= cfg.adrWidth), "Error: The width of addresses are too large", FAILURE)
  assert(Bool(isPow2(cfg.numOfRAMs) &&
              (cfg.numOfRAMs >= 2)), "Error: Number of RAMs has to be a power of 2 and at least 2", FAILURE)

  // I/O ports
  val io = new Bundle {

    // Instruction port (read only)
    val readDataAdr = in UInt (cfg.adrWidth bits)
    val readData    = out Bits (cfg.wordSize bits)

    // Memory port (write only)
    val writeEnable  = in Bool
    val writeDataAdr = in UInt (cfg.adrWidth bits)
    val writeData    = in Bits (cfg.wordSize bits)

  }.setName("")

  // Calculate the number of bits needed to address the RAMs
  def ramAdrWidth = log2Up(cfg.numOfRAMs)

  // Number of cells of a RAM
  def numOfCells = 1 << (cfg.adrWidth - ramAdrWidth)

  // Write a message
  println("[J1Sc] Create " + cfg.numOfRAMs + " RAMs which have " + numOfCells + " cells each")
  println("[J1Sc] Read " + cfg.bootCode().length + " words of the FORTH base system")

  // Create a complete list of memory blocks (start with first block)
  val ramList = for (i <- 0 to cfg.numOfRAMs - 1) yield {

    // Write a message
    println("[J1Sc] Fill RAM " + i + " ranging from " + (i * numOfCells) + " to " + (i * numOfCells + numOfCells - 1))

    // Create the ith RAM and fill it with the appropriate part of the bootcode
    Mem(Bits(cfg.wordSize bits), cfg.bootCode().slice(i * numOfCells, (i + 1) * numOfCells))

  }

  // Convert the list to a spinal vector
  val rPortsVec = Vec(for((ram,i) <- ramList.zipWithIndex) yield {

    // Create the write port of the ith RAM
    ram.write(enable  = io.writeEnable &&
                        (U(i) === io.writeDataAdr(io.writeDataAdr.high downto (io.writeDataAdr.high - ramAdrWidth + 1))),
              address = io.writeDataAdr((io.writeDataAdr.high - ramAdrWidth) downto 0),
              data    = io.writeData)

    // Create the read port of the ith RAM
    ram.readSync(address        = io.readDataAdr((io.readDataAdr.high - ramAdrWidth) downto 0),
                 readUnderWrite = readFirst)

  })

  // Multiplex the read port
  io.readData := rPortsVec(RegNext(io.readDataAdr(io.readDataAdr.high downto (io.readDataAdr.high - ramAdrWidth + 1))))

}
