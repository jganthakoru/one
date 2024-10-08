package com.mss.plugins;

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

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

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
import org.xml.sax.SAXException;

import com.mss.plugins.util.CodeQualityCheckUtility;

/**
 * Serves the Code Quality Check for CheckStyle and PMD
 * 
 * @author Jithendra Ganthakoru
 *
 */
@Mojo(name = "custom-checkstyle", defaultPhase = LifecyclePhase.VERIFY, requiresDependencyResolution = ResolutionScope.TEST)
public class CustomCheckstylePlugin extends AbstractMojo {

	@Parameter(defaultValue = "${project}", readonly = true, required = true)
	private MavenProject project;

	@Parameter(defaultValue = "${session}", readonly = true, required = true)
	private MavenSession session;

	@Component
	private BuildPluginManager pluginManager;

	@Parameter(property = "checkstyle.failOnViolation", defaultValue = "true")
	private boolean failOnViolation;

	@Parameter
	String[] rulesets;

	Log log = getLog();

	private List<String> checkStyleErrorList = new ArrayList<String>();

	private List<String> pmdErrorList = new ArrayList<String>();

	private final String checkStyleFileName = "checkstyle.xml";

	private final String pmdFileName = "pmd.xml";

	public void execute() throws MojoExecutionException {

		CodeQualityCheckUtility codeQualityCheck = new CodeQualityCheckUtility();
		try {

			URL checkStyleConfigURL = getClass().getClassLoader().getResource(checkStyleFileName);
			if (checkStyleConfigURL == null) {
				throw new MojoExecutionException("Unable to find checkstyle.xml");
			}
			checkStyleConfigURL.toString();
			Xpp3Dom checkStyleConfiguration = configuration(
					element(name("configLocation"), checkStyleConfigURL.toString()),
					element(name("failOnViolation"), Boolean.toString(failOnViolation)));

			executeMojo(
					plugin(groupId("org.apache.maven.plugins"), artifactId("maven-checkstyle-plugin"),
							version("3.4.0")),
					goal("check"), checkStyleConfiguration, executionEnvironment(project, session, pluginManager));

		} catch (Exception e) {
			try {
				checkStyleErrorList = codeQualityCheck.collectViolations("checkstyle", "target//checkstyle-result.xml",
						"error");
			} catch (ParserConfigurationException | SAXException | IOException e1) {
				e1.printStackTrace();
			}

			log.error("Error occurred while running checkstyle plugin " + e.getMessage());
		}

		// PMD implementation:
		try {

			URL pmdRulesetURL = getClass().getClassLoader().getResource(pmdFileName);

			if (pmdRulesetURL == null) {
				throw new MojoExecutionException("Unable to find " + pmdFileName);
			}

			rulesets = new String[] { getClass().getClassLoader().getResource(pmdFileName).toString() };

			Xpp3Dom pmdConfiguration = configuration();

			executeMojo(plugin(groupId("org.apache.maven.plugins"), artifactId("maven-pmd-plugin"), version("3.23.0")),
					goal("pmd"), pmdConfiguration, executionEnvironment(project, session, pluginManager));

			pmdErrorList = codeQualityCheck.collectViolations("pmd", "target//" + pmdFileName, "violation");

		} catch (Exception e) {

			log.error("Error occurred while running checkstyle plugin" + e.getMessage());
		}

		if (checkStyleErrorList.size() > 0 || pmdErrorList.size() > 0) {

			try {
				codeQualityCheck.writeReportsToHtml("target//error.html", checkStyleErrorList, pmdErrorList);
			} catch (IOException e) {
				log.error("Exception occurred while generating plugin for CheckStyle or PMD " + e.getMessage());
			}
			
			throw new MojoExecutionException("");

		}
	}

}