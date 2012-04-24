package domain;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyStoreException;
import java.util.ArrayList;
import java.util.ResourceBundle;

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
	private final ResourceBundle jiraProperties = ResourceBundle.getBundle("jira");
	private final String URL = jiraProperties.getString("url").trim();
	private final String USERNAME = jiraProperties.getString("username").trim();
	private final String PASSWORD = jiraProperties.getString("password").trim();

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
		System.out.println("Connecting to Jira...");
		this.factory = new JerseyJiraRestClientFactory();
		this.restClient = factory.createWithBasicHttpAuthentication(new URI(this.URL), this.USERNAME, this.PASSWORD);
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
