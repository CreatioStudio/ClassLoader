package vip.creatio.cloader.ccl.module;

import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class Downloader extends Thread {

    private static final Pattern FILE_NAME = Pattern.compile("attachment;(\\s+)?filename=([a-zA-Z0-9.\\-\"]+)");

    static final int BUFFER_SIZE = 4096;
    static final int TIMEOUT_SEC = 30;
    static final int TIMEOUT_SPEED_BYTE = 4096;
    static final int SPEED_CALC_BUFFER = 128;

    private final URL link;
    private final File dest;
    private final String default_name;

    private volatile long length = -1;

    private volatile File product;

    /** Process, 0~100, -100 means failed, -1 means connecting, -2 means idle */
    private volatile int process = -2;

    private volatile boolean started = false;


    private final int[] interval = new int[SPEED_CALC_BUFFER];

    private final int[] download_speed = new int[SPEED_CALC_BUFFER];

    private final long[] lastUpdated = new long[1];


    private volatile Thread self;
    private Runnable end_logic = () -> {};
    private ExceptionRunnable error_logic = (e) -> {};

    Downloader(URL link, File destFolder) {
        super();
        this.link = link;
        this.dest = destFolder;
        this.default_name = "autoGen_" + (int) (Math.random() * 4096);
    }

    Downloader(URL link, File destFolder, String default_name) {
        super();
        this.link = link;
        this.dest = destFolder;
        this.default_name = default_name;
    }

    public void run() {
        File temp1 = null;
        try {
            self = Thread.currentThread();
            process = -1;
            URLConnection con = link.openConnection();

            if (con.getContentType().equals("application/octet-stream")
                    || con.getContentType().equals("application/java-archive")) {
                length = con.getContentLengthLong();

                String name = con.getHeaderField("Content-Disposition");

                if (name == null) {
                    name = default_name;
                    if (con.getContentType().equals("application/java-archive")) name += ".jar";
                } else {
                    Matcher mtc = FILE_NAME.matcher(name);

                    if (!mtc.find()) {
                        throw new RuntimeException("Connection Header corrupt!");
                    }

                    name = mtc.group(2);
                    name = name.startsWith("\"") ? name.substring(1, name.length() - 1) : name;
                }

                final File temp = new File(dest, "$" + name);
                temp1 = temp;
                temp.getParentFile().mkdirs();
                product = new File(dest, name);

                started = true;

                InputStream is = con.getInputStream();
                FileOutputStream os = new FileOutputStream(temp, false);

                byte[] buf = new byte[BUFFER_SIZE];
                long cursor = 0;
                int len;

                int updateTime = 0;
                Timer timeout = new Timer("DownloadWatchDog", true);
                timeout.schedule(new TimerTask() {
                    @Override
                    public void run() {

                        int idle = 0;
                        while (getDownloadSpeed() < TIMEOUT_SPEED_BYTE) {
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException ignored) {}
                            idle++;
                            if (idle >= TIMEOUT_SEC) {
                                self.stop();
                                temp.delete();
                                error_logic.run(new RuntimeException("Download too slow!"));
                                this.cancel();
                                timeout.cancel();
                                System.gc();
                                break;
                            }
                        }
                    }
                }, 500L, 500L);
                while ((len = is.read(buf)) != -1) {
                    os.write(buf, 0, len);

                    cursor += len;
                    updateTime++;

                    process = (int) ((cursor * 100) / length);

                    if (updateTime % 10 == 0) {

                        System.arraycopy(interval, 0, interval, 1, SPEED_CALC_BUFFER - 1);
                        if (lastUpdated[0] != 0) interval[0] = (int) (System.nanoTime() - lastUpdated[0]);
                        lastUpdated[0] = System.nanoTime();

                        long sum = 0;
                        for (int i : interval) {
                            sum += i;
                        }
                        sum /= SPEED_CALC_BUFFER;

                        System.arraycopy(download_speed,0, download_speed, 1, download_speed.length - 1);
                        if (sum != 0) download_speed[0] = (int) (BUFFER_SIZE * 100_000_000L / sum);
                    }
                }
                is.close();
                os.close();
                process = 100;
                product.delete();
                temp.renameTo(product);
                end_logic.run();
                return;
            }
            error_logic.run(new RuntimeException("Invalid download link!"));
        } catch (Throwable e) {
            if (temp1 != null) temp1.delete();
            error_logic.run(e);
        }
    }


    public URL getLink() {
        return link;
    }

    public File getDest() {
        return dest;
    }

    public @Nullable File getProduct() {
        if (!started) return null;
        return product;
    }

    public int getProcess() {
        return process;
    }

    public long getContentLength() {
        return length;
    }

    public boolean isStarted() {
        return started;
    }

    public long getLength() {
        return length;
    }

    public String getDefaultFileName() {
        return default_name;
    }

    public int getDownloadSpeed() {
        long sum = 0;
        for (int i : download_speed) {
            sum += i;
        }
        return (int) (sum / SPEED_CALC_BUFFER);
    }

    void setEndLogic(Runnable runnable) {
        end_logic = runnable;
    }

    void setExceptionLogic(ExceptionRunnable runnable) {
        error_logic = runnable;
    }

    interface ExceptionRunnable {
        void run(Throwable t);
    }
}
