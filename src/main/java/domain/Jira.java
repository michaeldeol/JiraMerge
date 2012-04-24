package domain;

import java.io.FileInputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyStoreException;
import java.util.ArrayList;
import java.util.Properties;

import com.atlassian.jira.rest.client.JiraRestClient;
import com.atlassian.jira.rest.client.NullProgressMonitor;
import com.atlassian.jira.rest.client.domain.BasicIssue;
import com.atlassian.jira.rest.client.internal.jersey.JerseyJiraRestClientFactory;

public class Jira {
	
	/**
	 * Resource Bundle:
	 * Access to URL/UserName/Password for Jira
	 * 
	 * TODO This needs to be updated to use OAuth once implemented in the JRJC Library
	 */
	private final Properties prop = new Properties();
	private final JerseyJiraRestClientFactory factory;
	private final JiraRestClient restClient;
	private final NullProgressMonitor pm;
	
	/**
	 * Jira Object Constructor
	 * 
	 * @throws URISyntaxException
	 * @throws KeyStoreException 
	 */
	public Jira() throws URISyntaxException {
		try {
			prop.load(new FileInputStream("jira.properties"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("Connecting to Jira...");
		this.factory = new JerseyJiraRestClientFactory();
		this.restClient = factory.createWithBasicHttpAuthentication(new URI(
				prop.getProperty("url").trim()), 
				prop.getProperty("username").trim(), 
				prop.getProperty("password").trim());
		this.pm = new NullProgressMonitor();
	}
	
	/**
	 * getTickets(String, String):
	 * Search Jira using JQL for tickets that match the passed in args
	 * @param project
	 * @param version
	 * @return
	 */
	public ArrayList<String> getTickets(String project, String version) {
		System.out.println("Searching for tickets in Project: " + project + " Version: " + version);
		ArrayList<String> issuesFound = new ArrayList<String>();
		Iterable<BasicIssue> issues = restClient
				.getSearchClient()
				.searchJql(
						"project = \""
								+ project
								+ "\" AND fixVersion = \""
								+ version
								+ "\" AND Resolution = \"Fixed\" AND Status IN (\"Resolved\", \"Closed\") ORDER BY priority DESC",
						this.pm).getIssues();

		for (BasicIssue basicIssue : issues) {
			System.out.println("Found: " + basicIssue.getKey());
			issuesFound.add(basicIssue.getKey());
		}
		return issuesFound;
	}
	
}
