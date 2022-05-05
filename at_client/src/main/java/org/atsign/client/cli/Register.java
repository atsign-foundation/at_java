package org.atsign.client.cli;

import org.atsign.client.util.RegisterUtil;

public class Register {
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.err.println("Usage: Register <email@email.com>");
            System.exit(1);
        }
        String email = args[0];
        RegisterUtil registerUtil = new RegisterUtil();
        registerUtil.registerAtsign(email, registerUtil.getFreeAtsign());

    }

}
