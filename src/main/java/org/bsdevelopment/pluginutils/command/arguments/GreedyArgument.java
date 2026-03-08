package org.bsdevelopment.pluginutils.command.arguments;

/**
 * Marker interface for arguments that consume all remaining command tokens as a single value.
 *
 * <p>When {@link org.bsdevelopment.pluginutils.command.CommandBuilder} encounters a greedy
 * argument during execution, it joins every remaining token (from the argument's position to
 * the end of the input) with a single space and passes the combined string to
 * {@link Argument#parse}. Because of this, a greedy argument must always be placed
 * <b>last</b> in the command's argument list.
 *
 * @see GreedyStringArgument
 * @see StorageTagArgument
 */
public interface GreedyArgument {
}
