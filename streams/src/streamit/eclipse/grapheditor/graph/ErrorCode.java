/*
 * Created on Feb 20, 2004
 */
package streamit.eclipse.grapheditor.graph;

import javax.swing.JOptionPane;
import javax.swing.JPanel;

/**
 * Handles the errors that might occur in the application due to 
 * illegal actions by the user.
 * @author jcarlos
 */
public class ErrorCode 
{
	
	public static final int NO_ERROR = 0;
	public static final int CODE_EDGES_IN = -1;
	public static final int CODE_EDGES_OUT= -2;
	public static final int CODE_CONTAINER_CONNECTION = -3;
	public static final int CODE_CONNECT_TO_SELF = -4;
	public static final int CODE_NO_SPLITTER = -5;
	public static final int CODE_NO_JOINER = -6;
	public static final int CODE_NO_ANCESTOR_CONNECTION = -7;
	public static final int CODE_SPLITTER_ENDNODE_SJ = -8;
	public static final int CODE_SPLITTER_JOINER_CONNECT_SJ = -9;
	public static final int CODE_JOINER_SAME_PARENT_CONNECT_SJ = -10;
	public static final int CODE_INNERNODES_SJ = -11; 
	public static final int CODE_INVALID_PARENT_TYPE = -12;
	public static final int CODE_JOINER_NO_SAME_PARENT_CONNECT = -13;
	 
	public static final int CODE_NO_BODY_IN_FLOOP = -16;
	public static final int CODE_NO_LOOP_IN_FLOOP  = -17;
	public static final int CODE_SAME_BODY_AND_LOOP = -18;
	
	public static final int CODE_DIFFERENT_INPUT_OUPUT_TYPES = -19;
	public static final int CODE_CONNECTION_VOID = -20; 
	
	public static final int CODE_NO_ANCESTOR_IN_CONTAINER = -21;
	public static final int CODE_ANCESTOR_IN_CONTAINER=-22; 
	public static final int CODE_SPLITTER_MAX_CONNECTIONS=-23;
	public static final int CODE_JOINER_MAX_CONNECTIONS=-24;
	public static final int CODE_MISSING_CONNECTIONS_IN_PIPE=-25;
	 
	public static final String MESSAGE_EDGES_IN ="Cannot connect more than one edges into node";
	public static final String MESSAGE_EDGES_OUT ="Cannot connect more than one edges out from the node";
	public static final String MESSAGE_CONTAINER_CONNECTION ="Cannot connect to/from a container node";
	public static final String MESSAGE_CONNECT_TO_SELF = "Cannot connect node to itself";
	public static final String MESSAGE_NO_SPLITTER = "The Container is missing a splitter";
	public static final String MESSAGE_NO_JOINER = "The Container is missing a joiner";
	public static final String MESSAGE_NO_ANCESTOR_CONNECTION ="The target node in connection has no ancestor in source node's parent";
	public static final String MESSAGE_SPLITTER_ENDNODE_SJ = "The splitter cannot be the target of a connection where the source comes from same splitjoin";
	public static final String MESSAGE_SPLITTER_JOINER_CONNECT_SJ ="Cannot connect splitter to joiner in a splitjoin";
	public static final String MESSAGE_JOINER_SAME_PARENT_CONNECT_SJ="Cannot connect joiner to a target inside the same splitjoin";
	public static final String MESSAGE_INNERNODES_SJ = "Can only connect inner nodes of the splitjoin to its joiner";
	public static final String MESSAGE_INVALID_PARENT_TYPE = "Selected an invalid parent type";
	public static final String MESSAGE_JOINER_NO_SAME_PARENT_CONNECT = "The joiner must connect to a node in the same container node";
	public static final String MESSAGE_CODE_DIFFERENT_INPUT_OUPUT_TYPES = "The output type of the source node must equal the input type of the input node";
	public static final String MESSAGE_CONNECTION_VOID = "Cannot connect nodes that have a void input/output type";	
	public static final String MESSAGE_DEFAULT_ERROR="Invalid node connection";
	
	public static final String MESSAGE_NO_BODY_IN_FLOOP = "Must select a loop for the feedbackloop.";
	public static final String MESSAGE_NO_LOOP_IN_FLOOP = "Must select a body for the feedbackloop.";
	public static final String MESSAGE_SAME_BODY_AND_LOOP ="Cannot select the same body and loop for the feedbackloop";
	
	public static final String MESSAGE_NO_ANCESTOR_IN_CONTAINER="Cannot connect node since it has no ancestor in the container";
	public static final String MESSAGE_ANCESTOR_IN_CONTAINER="Cannot connect node since it has an ancestor in the container";
	public static final String MESSAGE_SPLITTER_MAX_CONNECTIONS="The splitter does not support any more connections";
	public static final String MESSAGE_JOINER_MAX_CONNECTIONS="The joiner does not support any more connections ";
	public static final String MESSAGE_MISSING_CONNECTIONS_IN_PIPE="A pipeline does not have all of its components connected";
	
	
	public static void handleErrorCode(int error)
	{
		String errorMessage = "";
		switch(error)
		{
			case CODE_EDGES_IN:{
				errorMessage = MESSAGE_EDGES_IN; 
				break;
			}
			case CODE_EDGES_OUT:{
				errorMessage = MESSAGE_EDGES_OUT;
				break;
			}
			case CODE_CONTAINER_CONNECTION:{
				errorMessage = MESSAGE_CONTAINER_CONNECTION;
				break;
			}
			case CODE_CONNECT_TO_SELF:{
				errorMessage = MESSAGE_CONNECT_TO_SELF;
				break;
			}
			case CODE_NO_SPLITTER:{
				errorMessage = MESSAGE_NO_SPLITTER;
				break;
			}
			case CODE_NO_JOINER:{
				errorMessage = MESSAGE_NO_JOINER;
				break;
			}
			case CODE_NO_ANCESTOR_CONNECTION:{
				errorMessage = MESSAGE_NO_ANCESTOR_CONNECTION;
				break;
			}
			case CODE_SPLITTER_ENDNODE_SJ:{
				errorMessage = MESSAGE_SPLITTER_ENDNODE_SJ;
				break;
			}
			case CODE_SPLITTER_JOINER_CONNECT_SJ:{
				errorMessage = MESSAGE_SPLITTER_JOINER_CONNECT_SJ;
				break;
			}
			case CODE_JOINER_SAME_PARENT_CONNECT_SJ:{
				errorMessage = MESSAGE_JOINER_SAME_PARENT_CONNECT_SJ;
				break;
			}
			case CODE_INNERNODES_SJ:{
				errorMessage = MESSAGE_INNERNODES_SJ;
				break;
			}
			case CODE_INVALID_PARENT_TYPE:{
				errorMessage = MESSAGE_INVALID_PARENT_TYPE;
				break;
			}
			case CODE_JOINER_NO_SAME_PARENT_CONNECT:{
				errorMessage = MESSAGE_JOINER_NO_SAME_PARENT_CONNECT;
				break;
			}
			case CODE_NO_BODY_IN_FLOOP:{
				errorMessage = MESSAGE_NO_BODY_IN_FLOOP;
				break;
			}
			case CODE_NO_LOOP_IN_FLOOP:{
				errorMessage = MESSAGE_NO_LOOP_IN_FLOOP;
				break;
			}
			case CODE_SAME_BODY_AND_LOOP:{
				errorMessage = MESSAGE_SAME_BODY_AND_LOOP;
				break;
			}
			case CODE_DIFFERENT_INPUT_OUPUT_TYPES:{
				errorMessage = MESSAGE_CODE_DIFFERENT_INPUT_OUPUT_TYPES;
				break;
			}
			case CODE_CONNECTION_VOID:{
				errorMessage = MESSAGE_CONNECTION_VOID;
				break;
			}
			case CODE_NO_ANCESTOR_IN_CONTAINER:{
				errorMessage = MESSAGE_NO_ANCESTOR_IN_CONTAINER;
				break;
			}
			case CODE_ANCESTOR_IN_CONTAINER:{
				errorMessage = MESSAGE_ANCESTOR_IN_CONTAINER;
				break;
			}
			case CODE_SPLITTER_MAX_CONNECTIONS:{
				errorMessage = MESSAGE_SPLITTER_MAX_CONNECTIONS;
				break;
			}

			case CODE_JOINER_MAX_CONNECTIONS:{
				errorMessage = MESSAGE_JOINER_MAX_CONNECTIONS;
				break;
			}
			
			case CODE_MISSING_CONNECTIONS_IN_PIPE:{
							errorMessage = MESSAGE_MISSING_CONNECTIONS_IN_PIPE;
							break;
						}

			default:{
				errorMessage = MESSAGE_DEFAULT_ERROR;
				break;
			}
		}
		JOptionPane.showMessageDialog(new JPanel(),
							errorMessage,
							"Error",
							JOptionPane.ERROR_MESSAGE);	
	}
}
