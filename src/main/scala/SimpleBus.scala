/*
 * Author: Steffen Reith (steffen.reith@hs-rm.de)
 *
 * Creation Date:  Sat Nov 12 15:36:19 GMT+1 2016
 * Module Name:    SimpleBus - A simple bus which consists out of address / dataIn / dataOut / writeMode / enable
 * Project Name:   J1Sc - A simple J1 implementation in Scala using Spinal HDL
 *
 * Remark: Initial code provided by Charles Papon (charles.papon.90@gmail.com)
 *
 */
import spinal.core._
import spinal.lib._
import spinal.lib.bus.misc._

case class SimpleBus(cfg : J1Config) extends Bundle with IMasterSlave {

  // Width of addresses and data
  def adrWidth  = cfg.wordSize
  def dataWidth = cfg.wordSize

  // Signals used by 'SimpleBus'
  val enable    = Bool // Bus can be used when 'enable' is high
  val writeMode = Bool // High to write data, low to read data
  val address   = UInt(adrWidth bits) // Address (byte-aligned)
  val writeData = Bits(dataWidth bits)
  val readData  = Bits(dataWidth bits)

  // Created a copy of the current bus which signals are delayed by 'delayCnt' ticks
  def delayed(delayCnt : Int = 1) : SimpleBus = {

    // Check for proper parameter
    require (delayCnt >= 0, "Error: delayCnt has to be at least 0")

    // Make a copy
    val retVal = cloneOf(this)

    // Don't delay the data to be read hence we have one wait state for read operation
    this.readData := retVal.readData

    // Delay all other signals and wire them
    retVal.address   := Delay(this.address,    delayCnt)
    retVal.enable    := Delay(this.enable,     delayCnt)
    retVal.writeMode := Delay(this.writeMode,  delayCnt)
    retVal.writeData := Delay(this.writeData,  delayCnt)

    // Return the delayed version of the actual SimpleBus object
    return retVal

  }

  // Methods to connect SimpleBus objects
  def << (that : SimpleBus) : Unit = {

    // Simply wire the signals of 'this' and 'that'
    that.enable    := this.enable
    that.writeMode := this.writeMode
    that.address   := this.address
    that.writeData := this.writeData
    this.readData  := that.readData

  }
  def >>(that : SimpleBus) : Unit = that << this

  // This is called by 'apply' when the master-object is called with data (-> side effect write/read data)
  override def asMaster() : Unit = {

    // Write data to the bus
    out(enable, writeMode, address, writeData)

    // Read data from the bus
    in(readData)

  }

}

case class SimpleBusSlaveFactory(bus : SimpleBus) extends BusSlaveFactoryDelayed {

  // Build the bridging logic between master and slave
  override def build() : Unit = {

    // Init the read data wire
    bus.readData := 0

    // The bus consists out of different BusSlaveFactoryElement's (iterate over the internal list of them)
    for(element <- elements) element match {

      // Only non-stop write mode is implement (can be e.g. used to send data of a Flow)
      case element : BusSlaveFactoryNonStopWrite =>

        // Write payload of 'getBitsWidth' bits at 'bitOffset' to the BusSlaveFactoryElement
        element.that.assignFromBits(bus.writeData(element.bitOffset, element.that.getBitsWidth bits))

      // Ignore all other write modes
      case _ =>

    }

    // Iterate over a map where the keys a addresses and the values are arrays of jobs to be done for that address
    when(bus.enable){
    
      switch(bus.address) {
      
        for((address, jobs) <- elementsPerAddress) {

          // Check whether the address matches and the bus is enabled
          is(address) {

            // Check for write - mode
            when(bus.writeMode) {

              // For all jobs regarding the given address (write mode)
              for (element <- jobs) element match {

                // Check of  write operation
                case element : BusSlaveFactoryWrite => {

                    // Write payload of 'getBitsWidth' bits at 'bitOffset' to the BusSlaveFactoryElement
                    element.that.assignFromBits(bus.writeData(element.bitOffset, element.that.getBitsWidth bits))

                }

                // Execute the action which is registered to a write job on the actual address
                case element : BusSlaveFactoryOnWriteAtAddress => element.doThat()

                // Ignore all other types of BusSlaveFactoryElements
                case _ =>

              }

            } otherwise {

              // For all jobs regarding the given address (read mode)
              for (element <- jobs) element match {

                // Check of a Read job
                case element : BusSlaveFactoryRead => {

                    // Read data on the bus
                    bus.readData(element.bitOffset, element.that.getBitsWidth bits) := element.that.asBits

                }

                // Execute the action which is registered to a read operation on the actual address
                case element : BusSlaveFactoryOnReadAtAddress => element.doThat()

                // Ignore all other types of BusSlaveFactoryElements
                case _ =>

              }

            }

          }
          
        }

      }

    }

  }

  // Tell the width of the data bus
  override def busDataWidth : Int = bus.dataWidth

}
