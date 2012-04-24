package domain;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Properties;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.PullCommand;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.errors.AmbiguousObjectException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

public class Repo {
	
	private final Properties prop = new Properties();
	private UsernamePasswordCredentialsProvider upc;
	
	private Git git;
	private Repository repository;
	private String branchName;
	
	private boolean successful = true;
	
	/**
	 * Repo object constructor
	 * 
	 * @throws IOException
	 */
	public Repo(String branchName) throws IOException {
		try {
			prop.load(new FileInputStream("git.properties"));
			this.upc = new UsernamePasswordCredentialsProvider(prop.getProperty("username").trim(), 
															   prop.getProperty("password").trim());
		} catch (Exception e) {
			e.printStackTrace();
		}
		this.branchName = branchName;
		// Setting up the Git Repo
		FileRepositoryBuilder builder = new FileRepositoryBuilder();
		this.repository = builder
				.setGitDir(new File(prop.getProperty("path").trim()))
				.readEnvironment() // scan environment GIT_* variables
				.findGitDir() // scan up the file system tree
				.build();
		this.git = new Git(repository);
	}
	
	/**
	 * connect():
	 * Setup the connection to the git repo object
	 * @throws IOException
	 * @throws JGitInternalException
	 * @throws InvalidRemoteException
	 */
	public void connect() throws IOException, JGitInternalException, InvalidRemoteException {
		System.out.println("Deleting remote branch: " + this.branchName);
		this.git.push().setRefSpecs(new RefSpec(":refs/heads/" + this.branchName)).setCredentialsProvider(this.upc).call();
		this.pull();
		this.create();
		this.checkout();
		if (!this.successful) this.onFailure();
	}
	
	/**
	 * disconnect():
	 * On repo disconnect, we do some cleanup
	 * @throws IOException
	 */
	public void disconnect() throws IOException {
		this.cleanUp();
		if (!this.successful) this.onFailure();
	}
	
	/**
	 * pull():
	 * Sets up a PullCommand object and logs in with the
	 * appropriate Credentials Provider
	 */
	private void pull() {
		System.out.println("Updating Master...");
		try {
			PullCommand pull = this.git.pull();
			pull.setCredentialsProvider(this.upc).call();
		} catch (Exception e) {
			System.out.println("Error: Unable to update master");
			e.printStackTrace();
			this.successful = false;
		}
	}
	
	/**
	 * create():
	 * Create a new branch with provided branchName
	 */
	private void create() {
		System.out.println("Creating Branch: " + this.branchName);
		try {
			this.git.branchCreate().setName(this.branchName).call();
		} catch (Exception e) {
			System.out.println("Error: Unable to create branch (" + this.branchName + ")");
			e.printStackTrace();
			this.successful = false;
		}
	}
	
	/**
	 * checkout():
	 * Checkout this.branchName on the git object
	 */
	private void checkout() {
		try {
			this.git.checkout().setName(this.branchName).call();
		} catch (Exception e) {
			System.out.println("Error: Unable to checkout branch (" + this.branchName + ")");
			e.printStackTrace();
			this.successful = false;
		}
	}
	
	/**
	 * mergeTickets(ArrayList<String>):
	 * Take the passed in ArrayList and search the repo for any tickets available.
	 * If we are able to find any branches, we then merge.
	 * @param issueBranches
	 * @throws AmbiguousObjectException
	 * @throws IOException
	 */
	public void mergeTickets(ArrayList<String> issueBranches) throws AmbiguousObjectException, IOException {
		// Loop through possible issue branches
		for (String string : issueBranches) {
			ObjectId branch = this.repository.resolve("refs/remotes/origin/" + string);
			// Verify branch exists
			if (branch == null) {
				System.out.println("Unable to resolve branch: " + string);
				this.successful = false;
			} else {
				// Try to merge found branches
				System.out.println("Merging Branch: " + string);
				MergeResult m = null;
				try {
					m = this.git.merge().include(branch).call();
				} catch (Exception e) {
					System.out.println("Error: Issue trying to merge branch (" + this.branchName + ")");
					e.printStackTrace();
					this.successful = false;
				}

				System.out.println(m.toString());
				Map<String, int[][]> allConflicts = m.getConflicts();
				// If conflicts found, output reason
				if (allConflicts != null && allConflicts.size() > 0) {
					this.successful = false;
					for (String path : allConflicts.keySet()) {
						int[][] c = allConflicts.get(path);
						System.out.println("Conflicts in file " + path);
						for (int i = 0; i < c.length; ++i) {
							System.out.println("  Conflict #" + i);
							for (int j = 0; j < (c[i].length) - 1; ++j) {
								if (c[i][j] >= 0) {
									System.out.println("    Chunk for "
											+ m.getMergedCommits()[j]
											+ " starts on line #" + c[i][j]);
								}
							}
						}
					}
				}
			}
		}
		if (this.successful) {
			this.onSuccess();
		} else {
			this.onFailure();
		}
	}
	
	/**
	 * cleanUp():
	 * Some simple cleanup to make sure we are always working with a clean directory
	 * @throws IOException
	 */
	private void cleanUp() throws IOException {
		this.git.reset().setMode(ResetType.HARD).call();
		try {
			this.git.checkout().setName("master").call();
		} catch (Exception e) {
			System.out.println("Error: Unable to checkout master");
			e.printStackTrace();
			this.successful = false;
		}
		try {
			this.git.branchDelete().setForce(true).setBranchNames(this.branchName).call();
		} catch (Exception e) {
			System.out.println("Error: Unable to delete branch (" + this.branchName + ")");
			e.printStackTrace();
			this.successful = false;
		}
	}
	
	/**
	 * onSuccess():
	 * When we find no conflicts, we push our merge back to the repo
	 * @throws IOException
	 */
	private void onSuccess() throws IOException {
		// If everything goes as planned, we can now push to origin
		System.out.println("Pushing " + this.branchName + " to Origin");
		try {
			this.git.push().setCredentialsProvider(this.upc).call();
		} catch (Exception e) {
			System.out.println("Error: Unable to push " + this.branchName + " to origin");
			e.printStackTrace();
			this.successful = false;
		}
	}

	/**
	 * onFailure():
	 * When there is a conflict, we cleanup the directory and exit
	 * @throws IOException
	 */
	private void onFailure() throws IOException {
		this.cleanUp();
		// Report error
		System.out.println("One ore more conflicts occured, aborting...");
		System.exit(1);
	}

}
