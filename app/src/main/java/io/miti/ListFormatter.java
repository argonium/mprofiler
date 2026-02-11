package io.miti;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Format a list of a list of string.
 */
public final class ListFormatter
{
    /** The maximum string length of each column. */
    private List<Integer> maxWidths = null;

    /**
     * Default constructor.
     */
    public ListFormatter()
    {
        super();
    }


    /**
     * Construct and initialize the data using a list of list of strings.
     *
     * @param list the data
     */
    public ListFormatter(final List<List<String>> list)
    {
        init(list);
    }


    private void init(final List<List<String>> list)
    {
        maxWidths = new ArrayList<Integer>(0);
        if ((list == null) || (list.size() < 1))
        {
            return;
        }

        final int rows = list.size();
        for (int i = 0; i < rows; ++i)
        {
            final List<String> children = list.get(i);
            if ((children == null) || (children.size() < 1))
            {
                continue;
            }

            final int numChildren = children.size();
            for (int j = 0; j < numChildren; ++j)
            {
                final String str = children.get(j);
                final int len = ((str == null) ? 0 : str.length());
                if (maxWidths.size() == j)
                {
                    maxWidths.add(Integer.valueOf(len));
                    if (maxWidths.size() < j)
                    {
                        throw new RuntimeException("J = " + j + " and size=" + maxWidths.size());
                    }
                }
                else
                {
                    if (len > maxWidths.get(j).intValue())
                    {
                        maxWidths.set(j, Integer.valueOf(len));
                    }
                }
            }
        }
    }


    /**
     * Merge the lines into a single string.
     *
     * @param data the list of strings
     * @return a single string
     */
    public static String getTextLine(final List<String> data)
    {
        StringBuilder sb = new StringBuilder(200);
        final int rows = data.size();
        for (int i = 0; i < rows; ++i)
        {
            sb.append(data.get(i)).append("\n");
        }

        return sb.toString();
    }


    /**
     * Format each line to use the expanded width.
     *
     * @param minSpace the minimum amount of space between columns
     * @param data the data to format
     * @return a list of strings
     */
    public List<String> format(final int minSpace, final List<List<String>> data)
    {
        List<String> results = new ArrayList<String>(10);

        final int rows = data.size();
        for (int i = 0; i < rows; ++i)
        {
            List<String> row = data.get(i);
            final int subrows = row.size();

            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < subrows; ++j)
            {
                final int targetLen = maxWidths.get(j) + minSpace;
                final String item = row.get(j);
                if (item != null)
                {
                    sb.append(item);
                }

                // If there's another column after this one, append spaces after the column
                if (j < (subrows - 1))
                {
                    int tempLen = (item == null) ? 0 : item.length();
                    while (tempLen < targetLen)
                    {
                        sb.append(" ");
                        ++tempLen;
                    }
                }
            }

            results.add(sb.toString());
        }

        return results;
    }


    /**
     * Print out the list of widths.
     */
    @SuppressWarnings("unused")
    private void printWidthList()
    {
        if (maxWidths == null)
        {
            System.out.println("Max Widths is null");
        }
        else
        {
            final int rows = maxWidths.size();
            for (int i = 0; i < rows; ++i)
            {
                System.out.println(String.format("#%d: %d", i+1, maxWidths.get(i)));
            }
        }
    }


    /**
     * Get a string representing a table of arbitrary fields.
     *
     * @param objects
     * @param columns
     * @param titles
     * @return
     */
    public String getTable(final List<?> objects, final String[] columns, final String[] titles)
    {
        List<List<String>> list = buildListList(objects, columns, titles);
        init(list);
        List<String> fmtd = format(5, list);
        return getTextLine(fmtd);
    }


    /**
     * Build the list of list of strings.
     *
     * @param objects the list of objects
     * @param columns the fields in each object
     * @param titles the title of each column
     * @return
     */
    private List<List<String>> buildListList(final List<?> objects,
                                             final String[] columns,
                                             final String[] titles)
    {
        // This is the list of list of strings, and is returned at the end
        final List<List<String>> list = new ArrayList<List<String>>(20);

        // Add the columns for the header row
        int size = titles.length;
        List<String> header = new ArrayList<String>(size);
        for (int i = 0; i < size; ++i)
        {
            header.add(titles[i]);
        }
        list.add(header);

        // Iterate over each row in the input list
        for (Object object : objects)
        {
            List<String> row = new ArrayList<String>(size);

            final int count = columns.length;
            for (int i = 0; i < count; ++i)
            {
                try
                {
                    Field f = object.getClass().getDeclaredField(columns[i]);
                    final String val = getValueFromField(f, object);
                    row.add(val);
                }
                catch (SecurityException e) {
                    e.printStackTrace();
                }
                catch (NoSuchFieldException e) {
                    e.printStackTrace();
                }
                catch (IllegalArgumentException e) {
                    e.printStackTrace();
                }
                catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }

            // Add the row to the output list
            list.add(row);
        }

        return list;
    }


    /**
     * Get the value from the field.
     *
     * @param field the field from the object
     * @param object the object with the value
     * @return the value from the field
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     */
    private String getValueFromField(final Field field,
                                     final Object object)
            throws IllegalArgumentException, IllegalAccessException
    {
        field.setAccessible(true);
        String value = null;
        Class<?> type = field.getType();
        String typeName = type.getName();
        if (typeName.equals("int"))
        {
            value = Integer.toString(field.getInt(object));
        }
        else if (typeName.equals("float"))
        {
            value = Float.toString(field.getFloat(object));
        }
        else if (typeName.equals("double"))
        {
            value = Double.toString(field.getDouble(object));
        }
        else if (typeName.equals("long"))
        {
            value = Long.toString(field.getLong(object));
        }
        else if (typeName.equals("boolean"))
        {
            value = Boolean.toString(field.getBoolean(object));
        }
        else if (typeName.equals("byte"))
        {
            value = Byte.toString(field.getByte(object));
        }
        else if (typeName.equals("char"))
        {
            value = Character.toString(field.getChar(object));
        }
        else if (typeName.equals("short"))
        {
            value = Short.toString(field.getShort(object));
        }
        else if (typeName.equals("java.lang.String"))
        {
            value = (String) field.get(object);
        }
        else if (typeName.equals("java.math.BigDecimal"))
        {
            BigDecimal bd = (BigDecimal) field.get(object);
            value = bd.toPlainString();
        }
        else if (typeName.equals("java.util.Date"))
        {
            Date date = (Date) field.get(object);
            value = getDateTimeString(date.getTime());
        }
        else
        {
            System.err.println("Unhandled type of " + typeName);
        }

        return value;
    }

    private static String getDateTimeString(final long time)
    {
        // Check the input
        if (time <= 0)
        {
            return "Invalid time (" + Long.toString(time) + ")";
        }

        // Convert the time into a Date object
        Date date = new Date(time);

        // Declare our formatter
        SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");

        // Return the date/time as a string
        return formatter.format(date);
    }
}