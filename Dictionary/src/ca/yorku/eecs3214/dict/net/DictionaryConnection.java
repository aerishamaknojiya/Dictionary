package ca.yorku.eecs3214.dict.net;

import ca.yorku.eecs3214.dict.model.Database;
import ca.yorku.eecs3214.dict.model.Definition;
import ca.yorku.eecs3214.dict.model.MatchingStrategy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.*;

public class DictionaryConnection {

	private static final int DEFAULT_PORT = 2628;
	private Socket socket;
	private PrintWriter out;
	private BufferedReader in;
	private Status status;
	private DictStringParser parser;
	private int code;

	/**
	 * Establishes a new connection with a DICT server using an explicit host and
	 * port number, and handles initial welcome messages. This constructor does not
	 * send any request for additional data.
	 *
	 * @param host Name of the host where the DICT server is running
	 * @param port Port number used by the DICT server
	 * @throws DictConnectionException If the host does not exist, the connection
	 *                                 can't be established, or the welcome messages
	 *                                 are not successful.
	 * @throws IOException
	 */
	public DictionaryConnection(String host, int port) throws DictConnectionException {

		// TODO Add your code here

		try {
			socket = new Socket(host, port);
			out = new PrintWriter(socket.getOutputStream(), true);
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			status = Status.readStatus(in);
			code = status.getStatusCode();
			if (status.isNegativeReply() || !socket.isConnected()) {
				throw new DictConnectionException();
			}

		}

		catch (Exception e) {
			throw new DictConnectionException();
		}

	}

	/**
	 * Establishes a new connection with a DICT server using an explicit host, with
	 * the default DICT port number, and handles initial welcome messages.
	 *
	 * @param host Name of the host where the DICT server is running
	 * @throws DictConnectionException If the host does not exist, the connection
	 *                                 can't be established, or the welcome messages
	 *                                 are not successful.
	 */
	public DictionaryConnection(String host) throws DictConnectionException {
		this(host, DEFAULT_PORT);
	}

	/**
	 * Sends the final QUIT message, waits for its reply, and closes the connection
	 * with the server. This function ignores any exception that may happen while
	 * sending the message, receiving its reply, or closing the connection.
	 */
	public synchronized void close() {
		try {
			out.println("QUIT");
			in.readLine();
			in.close();
			out.close();
			socket.close();
		} catch (IOException e) {
		}

	}

	/**
	 * Requests and retrieves a map of database name to an equivalent database
	 * object for all valid databases used in the server.
	 *
	 * @return A map linking database names to Database objects for all databases
	 *         supported by the server, or an empty map if no databases are
	 *         available.
	 * @throws DictConnectionException If the connection is interrupted or the
	 *                                 messages don't match their expected value.
	 */
	public synchronized Map<String, Database> getDatabaseList() throws DictConnectionException {
		Map<String, Database> databaseMap = new HashMap<>();
		List<String> temp = new ArrayList<>();
		String str = "";
		String err = "";
		List<String> arr = new ArrayList<>();
		try {
			out.println("SHOW DB");

			str = in.readLine();
			err = str.substring(0, 3);
			if (err.equals("554")) {
				return databaseMap;
			}

			str = in.readLine();

			while (str != null) {
				temp.add(str);
				str = in.readLine();
				if (!in.ready())
					break;
			}

			for (int i = 0; i < temp.size() - 1; i++) {
				arr = DictStringParser.splitAtoms(temp.get(i));
				Database database = new Database(arr.get(0), arr.get(1));
				databaseMap.put(arr.get(0), database);
			}

		} catch (Exception e) {
			throw new DictConnectionException();
		}

		return databaseMap;
	}

	/**
	 * Requests and retrieves a list of all valid matching strategies supported by
	 * the server. Matching strategies are used in getMatchList() to identify how to
	 * suggest words that match a specific pattern. For example, the "prefix"
	 * strategy suggests words that start with a specific pattern.
	 *
	 * @return A set of MatchingStrategy objects supported by the server, or an
	 *         empty set if no strategies are supported.
	 * @throws DictConnectionException If the connection was interrupted or the
	 *                                 messages don't match their expected value.
	 */
	public synchronized Set<MatchingStrategy> getStrategyList() throws DictConnectionException {
		Set<MatchingStrategy> set = new LinkedHashSet<>();
		List<String> temp = new ArrayList<>();
		String str = "";
		String err = "";
		List<String> arr = new ArrayList<>();
		try {
			out.println("SHOW STRAT");

			str = in.readLine();
			err = str.substring(0, 3);
			if (err.equals("555")) {
				return set;
			}

			str = in.readLine();

			while (str != null) {
				temp.add(str);
				str = in.readLine();
				if (!in.ready())
					break;
			}

			for (int i = 0; i < temp.size() - 1; i++) {
				arr = DictStringParser.splitAtoms(temp.get(i));
				MatchingStrategy ms = new MatchingStrategy(arr.get(0), arr.get(1));
				set.add(ms);
			}

		} catch (Exception e) {
			throw new DictConnectionException();
		}

		return set;
	}

	/**
	 * Requests and retrieves a list of matches for a specific word pattern.
	 *
	 * @param pattern  The pattern to use to identify word matches.
	 * @param strategy The strategy to be used to compare the list of matches.
	 * @param database The database where matches are to be found. Special databases
	 *                 like Database.DATABASE_ANY or Database.DATABASE_FIRST_MATCH
	 *                 are supported.
	 * @return A set of word matches returned by the server based on the word
	 *         pattern, or an empty set if no matches were found.
	 * @throws DictConnectionException If the connection was interrupted, the
	 *                                 messages don't match their expected value, or
	 *                                 the database or strategy are not supported by
	 *                                 the server.
	 */
	public synchronized Set<String> getMatchList(String pattern, MatchingStrategy strategy, Database database)
			throws DictConnectionException {
		Set<String> set = new LinkedHashSet<>();

		List<String> temp = new ArrayList<>();
		String str = "";
		String err = "";
		List<String> arr = new ArrayList<>();
		try {
			out.println("MATCH " + database.getName() + " " + strategy.getName() + " \"" + pattern + "\"");

			str = in.readLine();
			err = str.substring(0, 3);
			if (err.equals("552")) {
				return set;
			}
			if (err.equals("550") || err.equals("554") || err.equals("551")) {
				throw new DictConnectionException();
			}

			str = in.readLine();
			while (str != null) {
				temp.add(str);
				str = in.readLine();
				if (!in.ready())
					break;
			}

			for (int i = 0; i < temp.size() - 1; i++) {
				arr = DictStringParser.splitAtoms(temp.get(i));
				set.add(arr.get(1));
			}

		} catch (Exception e) {
			throw new DictConnectionException();
		}
		return set;
	}

	/**
	 * Requests and retrieves all definitions for a specific word.
	 *
	 * @param word     The word whose definition is to be retrieved.
	 * @param database The database to be used to retrieve the definition. Special
	 *                 databases like Database.DATABASE_ANY or
	 *                 Database.DATABASE_FIRST_MATCH are supported.
	 * @return A collection of Definition objects containing all definitions
	 *         returned by the server, or an empty collection if no definitions were
	 *         available.
	 * @throws DictConnectionException If the connection was interrupted, the
	 *                                 messages don't match their expected value, or
	 *                                 the database is not supported by the server.
	 */
	public synchronized Collection<Definition> getDefinitions(String word, Database database)
			throws DictConnectionException {
		// The implementation of this method correctly outputs the definitions on the GUI. However, due to some reasons I am not able to pass the test cases.
		Collection<Definition> set = new ArrayList<>();

        StringBuilder sb = null;
        Definition definition;
        boolean isDefinitionsRetrieved = false;
        String wName = "", DbName = "";
        ArrayList<String> temp = new ArrayList<>();

        try {

            out.println("DEFINE" + " " + database.getName() + " \"" + word + "\"");

            String line = in.readLine();

            if (line.split(" ", 2)[0].equals("550")) {
                throw new DictConnectionException();
            }
            if (line.split(" ", 2)[0].equals("552")) {

                return set;
            }

            while (line != null) {
                temp.add(line);

                line = in.readLine();
                if (!in.ready()) break;
            }
            
            

            for (int i = 0; i < temp.size(); i++) {

                String[] DefinitionContent = temp.get(i).split("\"");
           
                if (DefinitionContent[0].trim().equals("151")) {
                    isDefinitionsRetrieved = true;
                    if (sb != null) {
                        definition = new Definition(wName, DbName);
                        int t = sb.reverse().indexOf(".");
                        sb.deleteCharAt(t);
                        sb.reverse();
                        definition.setDefinition(sb.toString());
                        set.add(definition);
                    }
                    sb = new StringBuilder();
                    wName = DefinitionContent[1].trim();
                    DbName = DefinitionContent[2].trim();
                    continue;
                }
                if (isDefinitionsRetrieved) {
                    sb.append(temp.get(i));
                    sb.append("\n");
                }

            }
            if (isDefinitionsRetrieved) {
                definition = new Definition(wName, DbName);
                int t = sb.reverse().indexOf(".");
                sb.deleteCharAt(t);
                sb.reverse();
                definition.setDefinition(sb.toString());
                set.add(definition);
            }

        } catch (Exception e) {
            throw new DictConnectionException(e.getMessage());
        }

        return set;
	}

}