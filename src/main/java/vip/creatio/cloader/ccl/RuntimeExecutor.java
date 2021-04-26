package vip.creatio.cloader.ccl;

import org.bukkit.command.CommandSender;
import vip.creatio.cloader.bukkit.CLoader;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class RuntimeExecutor extends Thread
{
    AtomicReference<String> op = new AtomicReference<>();
    AtomicReference<CommandSender> lastOperator = new AtomicReference<>();
    AtomicLong lastExec = new AtomicLong(0L);
    Process proc = null;

    //Statistics
    private final int MAX_IDLE_INTERVAL = 60_000;

    private final int[] processCpuUsed = new int[90];
    private final int[] systemCpuUsed = new int[90];

    public RuntimeExecutor()
    {
        Arrays.fill(processCpuUsed, -1);

        //2s timer
        Timer timer = new Timer("RuntimeExecutorTimer2s", true);
        timer.schedule(new TimerTask()
        {
            private int timer = 0;
            @Override
            public void run()
            {
                //Process idle countdown
                if (proc != null && proc.isAlive())
                {
                    if (System.currentTimeMillis() - lastExec.get() >= MAX_IDLE_INTERVAL)
                    {
                        CommandSender sender = lastOperator.get();
                        CLoader.getMsgSender().sendStatic(
                                sender,
                                "MAIN.DOS.TIMEOUT");
                        proc = null;
                        lastOperator = new AtomicReference<>();
                    }
                }

                //Statistics
                if (timer % 5 == 0)
                {
                    System.arraycopy(processCpuUsed, 0, processCpuUsed, 1, processCpuUsed.length - 1);
                    processCpuUsed[0] = (int) FileManager.OSINFO.getProcessCpuLoad() * 100;

                    System.arraycopy(systemCpuUsed, 0, systemCpuUsed, 1, systemCpuUsed.length - 1);
                    systemCpuUsed[0] = (int) FileManager.OSINFO.getSystemCpuLoad() * 100;
                }
                timer++;
            }
        }, 0L, 2000L);
    }

    public void getNew(CommandSender sender, String initCmd)
            throws IOException
    {
        proc = Runtime.getRuntime().exec(initCmd);
        lastExec.set(System.currentTimeMillis());
        lastOperator.set(sender);
        msgOutput();
    }

    public boolean isProcessing()
    {
        return proc != null && proc.isAlive();
    }

    public void input(CommandSender sender, String cmd)
    {
        lastOperator.set(sender);
        lastExec.set(System.currentTimeMillis());
        op.set(cmd);
        interrupt();
    }

    @Override
    public void run()
    {
        while (proc != null && proc.isAlive())
        {
            try
            {
                wait();
            }
            catch (InterruptedException ignored) {}

            System.out.println("Interrupted");

            OutputStream o = proc.getOutputStream();
            PrintStream stream = new PrintStream(o);
            stream.print(op.get());
            stream.flush();
            stream.close();

            System.out.println("Inputing...");

            msgOutput();
        }
    }

    /** Get system cpu used from last 10s, 1min, 5min and 15 min */
    public double[] systemCpuUsed()
    {
        synchronized (systemCpuUsed)
        {
            return getCpuUsed0(systemCpuUsed);
        }
    }

    /** Get process cpu used from last 10s, 1min, 5min and 15 min */
    public double[] processCpuUsed()
    {
        synchronized (processCpuUsed)
        {
            return getCpuUsed0(processCpuUsed);
        }
    }

    //Cpu used time impl.
    private double[] getCpuUsed0(int[] processCpuUsed)
    {
        double[] used = new double[4];
        int total = 0;

        used[0] = processCpuUsed[0];
        for (int i = 0; i < processCpuUsed.length; i++)
        {
            if (processCpuUsed[i] == -1) {
                if (i == 5) used[1] = used[0];
                if (i == 29) used[2] = used[1];
                if (i == 89) used[3] = used[2];
            } else {
                total += processCpuUsed[i];
                if (i == 5) used[1] = total / 6.0;
                if (i == 29) used[2] = total / 30.0;
                if (i == 89) used[3] = total / 90.0;
            }
        }
        return used;
    }

    public String[] getOutput()
    {
        BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
        BufferedReader err = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
        List<String> str = new ArrayList<>();
        String line;
        try {
            while ((line = reader.readLine()) != null)
            {
                str.add(line);
            }

            while ((line = err.readLine()) != null)
            {
                str.add(line);
            }
        }
        catch (IOException e)
        {
            CLoader.intern("Exception while reading DOS output!");
            e.printStackTrace();
        }
        return str.toArray(new String[0]);
    }

    private void msgOutput()
    {
        System.out.println("Getting output");
        String[] msg = getOutput();
        for (String str : msg)
        {
            System.out.println("output: " + str);
            CLoader.getMsgSender().sendStatic(lastOperator.get(), "MAIN.DOS.OUTPUT", str);
        }
    }
}
