/*******************************************************************************
 * Copyright 2012
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package de.tudarmstadt.ukp.clarin.webanno.brat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.persistence.NoResultException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.model.Authority;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.User;

public class ApplicationUtils
{

    private static final Log LOG = LogFactory.getLog(ApplicationUtils.class);

    /**
     * Read Tag and Tag Description. A line has a tag name and a tag description separated by a TAB
     *
     */
    public static Map<String, String> getTagsFromText(String aLineSeparatedTags)
    {
        Map<String, String> tags = new HashMap<String, String>();
        StringTokenizer st = new StringTokenizer(aLineSeparatedTags, "\n");
        while (st.hasMoreTokens()) {
            StringTokenizer stTag = new StringTokenizer(st.nextToken(), "\t");
            String tag = stTag.nextToken();
            String description;
            if (stTag.hasMoreTokens()) {
                description = stTag.nextToken();
            }
            else {
                description = tag;
            }
            tags.put(tag.trim(), description);
        }
        return tags;
    }

    /**
     * IS user super Admin
     */
    public static boolean isSuperAdmin(RepositoryService aProjectRepository, User aUser)
    {
        boolean roleAdmin = false;
        List<Authority> authorities = aProjectRepository.getAuthorities(aUser);
        for (Authority authority : authorities) {
            if (authority.getRole().equals("ROLE_ADMIN")) {
                roleAdmin = true;
                break;
            }
        }
        return roleAdmin;
    }

    /**
     * Determine if the User is allowed to update a project
     *
     * @param aProject
     * @return
     */
    public static boolean isProjectAdmin(Project aProject, RepositoryService aProjectRepository,
            User aUser)
    {
        boolean roleAdmin = false;
        List<Authority> authorities = aProjectRepository.getAuthorities(aUser);
        for (Authority authority : authorities) {
            if (authority.getRole().equals("ROLE_ADMIN")) {
                roleAdmin = true;
                break;
            }
        }

        boolean projectAdmin = false;
        if (!roleAdmin) {
            try {
                if (aProjectRepository.getPermisionLevel(aUser, aProject).equals("admin")) {
                    projectAdmin = true;
                }
            }
            catch (NoResultException ex) {
                LOG.info("No permision is given to this user " + ex);
            }
        }

        return (projectAdmin || roleAdmin);
    }

    /**
     * Determine if the User is a curator or not
     *
     * @param aProject
     * @return
     */
    public static boolean isCurator(Project aProject, RepositoryService aProjectRepository,
            User aUser)
    {
        boolean roleAdmin = false;
        List<Authority> authorities = aProjectRepository.getAuthorities(aUser);
        for (Authority authority : authorities) {
            if (authority.getRole().equals("ROLE_ADMIN")) {
                roleAdmin = true;
                break;
            }
        }

        boolean curator = false;
        if (!roleAdmin) {
            try {
                if (aProjectRepository.getPermisionLevel(aUser, aProject).equals("curator")) {
                    curator = true;
                }
            }
            catch (NoResultException ex) {
                LOG.info("No permision is given to this user " + ex);
            }
        }

        return (curator || roleAdmin);
    }

    /**
     * Determine if the User is member of a project
     *
     * @param aProject
     * @return
     */
    public static boolean isMember(Project aProject, RepositoryService aProjectRepository,
            User aUSer)
    {
        boolean roleAdmin = false;
        List<Authority> authorities = aProjectRepository.getAuthorities(aUSer);
        for (Authority authority : authorities) {
            if (authority.getRole().equals("ROLE_ADMIN")) {
                roleAdmin = true;
                break;
            }
        }

        boolean member = false;
        if (!roleAdmin) {
            try {
                if (aProjectRepository.getPermisionLevel(aUSer, aProject).equals("user")) {
                    member = true;
                }
            }
            catch (NoResultException ex) {
                LOG.info("No permision is given to this user " + ex);
            }
        }

        return (member || roleAdmin);
    }
}