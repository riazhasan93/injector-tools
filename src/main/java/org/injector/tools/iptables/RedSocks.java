package org.injector.tools.iptables;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.injector.tools.config.RedSocksConfig;
import org.injector.tools.log.Logger;
import org.injector.tools.utils.PlatformUtil;
import org.injector.tools.utils.Utils;

public class RedSocks {

    private RedSocksConfig config;

    private File watch ;

    public RedSocks(RedSocksConfig config) {
        this.config = config;
        watch = new File(this.config.getDirectory() + "/watch.lock" );
        init();
    }
    private void init(){

        updateWatch();
        copyFiles();
        if (config.getState() == RedSocksConfig.RedSocksState.start){
            start();
        }else {
            stop();
        }


    }

    private void updateWatch(){
        try {
            watch.createNewFile();
                FileUtils.touch(watch);
        }catch (IOException e){
            Logger.debug(getClass(), "error touch - "+ watch.toString());
        }
    }
    private void copyFiles(){
        File destination = null;
        String redsocks = "";
        if(PlatformUtil.isUnix()) {
            redsocks += PlatformUtil.isAMD64() ? "amd64":"i386";
            redsocks += "/redsocks";
            URL source = getClass().getResource(redsocks);
            destination = new File(config.getDirectory(), "redsocks");
            Utils.Copy( source, destination );
            destination.setExecutable(true);
            Logger.debug(getClass(), "copy - "+ destination.toString());

            source = getClass().getResource("iptables.sh");
            destination = new File(config.getDirectory(), "iptables.sh");
            Utils.Copy( source, destination );
            destination.setExecutable(true);
            Logger.debug(getClass(), "copy - "+ destination.toString());



        }else if(PlatformUtil.isWindows()) {
            return;
        }else if(PlatformUtil.isMac()) {
            return;
        }


    }

    // {"sudo",  "-S", "./redsocks.sh" } #sudo /redsocks.sh dir start socks5 127.0.0.1 1080 false
    public void start(){
        List<String> args = new ArrayList<String>();
        args.add("gksu");
        args.add("-S");
        args.add(config.getDirectory()+"/iptables.sh");
        args.add(config.getDirectory());
        args.add(config.getState().toString());
        args.add(config.getProxyType().toString());
        args.add(config.getProxyHost());
        args.add(config.getProxyPort()+"");
        args.add(Boolean.toString(config.isUseAuth()));
        if (config.isUseAuth()){
            args.add(config.getProxyUser());
            args.add(config.getProxyPass());
        }
        Logger.debug(getClass(), "Need privileged access to run iptables chain");
        Logger.debug(getClass(), "Run redsocks on (127.0.0.1:4123)");
        Logger.debug(getClass(), "start iptables rules to redirect all traffic to use ssh socks5 proxy");
        runProcess(args);
    }

    public void stop(){
//        List<String> args = new ArrayList<String>();
//        args.add("gksu");
//        args.add("-S");
//        args.add(config.getDirectory()+"/iptables.sh");
//        args.add(config.getDirectory());
//        args.add(RedSocksConfig.RedSocksState.stop.toString());
//        runProcess(args);
//        Logger.debug(getClass(), Arrays.toString(args.toArray()));

        updateWatch();
        config.setState(RedSocksConfig.RedSocksState.stop);

    }


    private void runProcess(List<String> args) {
        ProcessBuilder processBuilder;
        processBuilder = new ProcessBuilder(args);
        try {
            processBuilder.start();
            Logger.debug(getClass(), "Start using iptables chain");
        } catch (IOException e) {
            Logger.debug(getClass(), "error in open " + e.getMessage());
        }
    }


    public RedSocksConfig getConfig() {
        return config;
    }
    public void setConfig(RedSocksConfig config) {
        this.config = config;
    }
}
