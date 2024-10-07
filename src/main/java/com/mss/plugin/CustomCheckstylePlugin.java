package com.mss.plugin;

import static org.twdata.maven.mojoexecutor.MojoExecutor.artifactId;
import static org.twdata.maven.mojoexecutor.MojoExecutor.configuration;
import static org.twdata.maven.mojoexecutor.MojoExecutor.element;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executeMojo;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executionEnvironment;
import static org.twdata.maven.mojoexecutor.MojoExecutor.goal;
import static org.twdata.maven.mojoexecutor.MojoExecutor.groupId;
import static org.twdata.maven.mojoexecutor.MojoExecutor.name;
import static org.twdata.maven.mojoexecutor.MojoExecutor.plugin;
import static org.twdata.maven.mojoexecutor.MojoExecutor.version;

import java.io.File;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;

@Mojo(name = "custom-checkstyle", defaultPhase = LifecyclePhase.VERIFY, requiresDependencyResolution = ResolutionScope.TEST)
public class CustomCheckstylePlugin extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Parameter(defaultValue = "${session}", readonly = true, required = true)
    private MavenSession session;

    @Component
    private BuildPluginManager pluginManager;

    private String configLocation;
    

    @Parameter(property = "checkstyle.failOnViolation", defaultValue = "true")
    private boolean failOnViolation;
    
    Log log = getLog();
    
    public void execute() throws MojoExecutionException{
        
    	try {
        
    		configLocation = getClass().getClassLoader().getResource("checkstyle-rules.xml").toString();
    		
            Xpp3Dom configuration = configuration( element(name("configLocation"), configLocation), element(name("failOnViolation"), Boolean.toString(failOnViolation)));
            
            executeMojo(plugin(groupId("org.apache.maven.plugins"),artifactId("maven-checkstyle-plugin"),version("3.2.1")),goal("check"),configuration,
                executionEnvironment(project,session,pluginManager));
            
            
            executeMojo(plugin(groupId("org.apache.maven.plugins"),artifactId("maven-pmd-plugin"),version("3.16.0")),goal("check"),configuration,
                    executionEnvironment(project,session,pluginManager));
            
            
            File targetDir = new File(project.getBuild().getDirectory());
            
            File checkstyleOutput = new File(targetDir, "checkstyle-result.xml");
            
            if (checkstyleOutput.exists()) {
            	
            	log.error(checkstyleOutput.toString());
                
            } else {
            	log.info("No Checkstyle violations found.");
            }
        
    	} catch (Exception e) {
    		
    		throw new MojoExecutionException("Error executing Checkstyle plugin", e);
        }
    }
}
