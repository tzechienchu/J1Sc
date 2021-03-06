--------------------------------------------------------------------------------
-- Author: <AUTHORNAME> (<AUTHOREMAIL>)
-- Committer: <COMMITTERNAME>
--
-- Creation Date:  Thu Oct 13 20:44:40 GMT+2 2016
-- Creator:        Steffen Reith
-- Module Name:    J1SoC_TB - A simple testbench for the J1 SoC
-- Project Name:   J1Sc - A simple J1 implementation in scala
--
-- Hash: <COMMITHASH>
-- Date: <AUTHORDATE>
--------------------------------------------------------------------------------
library ieee;
use ieee.std_logic_1164.all;
use ieee.numeric_std.all;

library std;
use std.textio.all;

entity J1SoC_tb is
end J1SoC_tb;

architecture Behavioral of J1SoC_tb is

  component J1SoC is
  
    port(reset     : in std_logic;
         clk100Mhz : in std_logic;
         extInt    : in std_logic_vector(0 downto 0);
         leds      : out std_logic_vector(15 downto 0);
         rx        : in std_logic;
         tx        : out std_logic);
  
  end component;

  -- Clock period definition (100Mhz)
  constant clk_period : time := 10 ns;

  -- Interrupts
  signal extInt : std_logic_vector(0 downto 0) := "0";
 
  -- UART signals
  signal rx : std_logic := '0';
  signal tx : std_logic;

  -- I/O signals 
  signal leds : std_logic_vector(15 downto 0);

  -- Clock and reset 
  signal clk100Mhz : std_logic;
  signal reset : std_logic;

begin

  uut : J1SoC
    port map (clk100Mhz => clk100Mhz,
              reset     => reset,
              extInt    => extInt,
              rx        => rx,
              tx        => tx,
              leds      => leds);

  -- Clock process definitions
  clk_process : process
  begin

    clk100Mhz <= '0';
    wait for clk_period/2;

    clk100Mhz <= '1';
    wait for clk_period/2;

  end process;
  
  reboot_proc : process
  begin

    -- Reset the CPU (asynchronous) 
    reset <= '1';

    -- Wait 57ns
    wait for 57 ns;

    -- Revoke the the reset
    reset <= '0';

    -- Wait forever  
    wait;

  end process;


  -- Stimulus process
  stim_proc : process

    -- Text I/O
    variable lineBuffer : line;

  begin

    -- Give a info message
    write(lineBuffer, string'("Start the simulation of the CPU"));
    writeline(output, lineBuffer);

    -- Simply wait forever
    wait;

  end process;

end architecture;
