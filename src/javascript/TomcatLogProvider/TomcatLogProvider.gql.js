import {gql} from '@apollo/client';

export const GET_SETTINGS = gql`
    query {
        tomcatLog {
            settings {
                mountPath
                logPath
            }
        }
    }
`;

export const SAVE_SETTINGS = gql`
    mutation TomcatLogSaveSettings($mountPath: String!) {
        tomcatLog {
            saveSettings(mountPath: $mountPath)
        }
    }
`;

export const GET_LOG_TAIL = gql`
    query {
        tomcatLog {
            tail(lines: 200)
        }
    }
`;
