package client;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;

import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.JGitInternalException;

import domain.Jira;
import domain.Repo;

public class JiraMerge {
	
	public static void main(String[] args) throws URISyntaxException, IOException, JGitInternalException, InvalidRemoteException {

		// Create a Jira object and grab the tickets required.
		Jira jira = new Jira();
		ArrayList<String> tickets = jira.getTickets(args[0], args[1]);
	
		// If we find any tickets, we can then start our merge
		if (tickets.size() > 0) {
			// Create a Repo object and merge the avail tickets.
			Repo repo = new Repo(args[1]);
			repo.connect();
			repo.mergeTickets(tickets);
			repo.disconnect();			
		} else {
			System.out.println("Error: No tickets found, aborting...");
			System.exit(1);
		}
	}

}