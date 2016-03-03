package com.gitblit.wicket.panels;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.model.IModel;

import com.gitblit.utils.StringUtils;
import com.gitblit.wicket.WicketUtils;

/**
 * A re-usable password text field option panel.
 *
 * @author Yaroslav Pankratyev
 */
public class PasswordOption extends BasePanel {

    private static final long serialVersionUID = 1L;

    public PasswordOption(String wicketId, String title, String description, IModel<String> model) {
        this(wicketId, title, description, null, model);
    }

    public PasswordOption(String wicketId, String title, String description, String css, IModel<String> model) {
        super(wicketId);
        add(new Label("name", title));
        add(new Label("description", description).setVisible(!StringUtils.isEmpty(description)));
        PasswordTextField tf = new PasswordTextField("text", model);
        tf.setResetPassword(false);
        tf.setRequired(false);
        if (!StringUtils.isEmpty(css)) {
            WicketUtils.setCssClass(tf, css);
        }
        add(tf);
    }
}
