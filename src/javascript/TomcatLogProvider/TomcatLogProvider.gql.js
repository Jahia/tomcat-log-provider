import {gql} from '@apollo/client';

export const GET_SETTINGS = gql`
    query {
        tomcatLogSettings {
            mountPath
            logPath
        }
    }
`;

export const SAVE_SETTINGS = gql`
    mutation TomcatLogSaveSettings($mountPath: String!) {
        tomcatLogSaveSettings(mountPath: $mountPath)
    }
`;
