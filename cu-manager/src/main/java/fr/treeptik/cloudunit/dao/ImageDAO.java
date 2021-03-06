/*
 * LICENCE : CloudUnit is available under the Gnu Public License GPL V3 : https://www.gnu.org/licenses/gpl.txt
 *     but CloudUnit is licensed too under a standard commercial license.
 *     Please contact our sales team if you would like to discuss the specifics of our Enterprise license.
 *     If you are not sure whether the GPL is right for you,
 *     you can always test our software under the GPL and inspect the source code before you contact us
 *     about purchasing a commercial license.
 *
 *     LEGAL TERMS : "CloudUnit" is a registered trademark of Treeptik and can't be used to endorse
 *     or promote products derived from this project without prior written permission from Treeptik.
 *     Products or services derived from this software may not be called "CloudUnit"
 *     nor may "Treeptik" or similar confusing terms appear in their names without prior written permission.
 *     For any questions, contact us : contact@treeptik.fr
 */

package fr.treeptik.cloudunit.dao;

import fr.treeptik.cloudunit.model.Image;
import org.springframework.dao.DataAccessException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ImageDAO
    extends JpaRepository<Image, Integer> {

    @Query("Select i from Image i where i.name=:name")
    Image findByName(@Param("name") String name)
        throws DataAccessException;

    @Query("select i from Image i where i.status=1")
    List<Image> findAllEnabledImages()
        throws DataAccessException;

    @Query("select i from Image i where i.status=1 and i.imageType=:imageType")
    List<Image> findAllEnabledImagesByType(@Param("imageType") String imageType)
        throws DataAccessException;

    @Query("select count(m) from Module m left join m.application a left join m.image i where i.name LIKE %:name and a.name=:appName and a.user.login=:userLogin ")
    Long countNumberOfInstances(@Param("name") String moduleName,
                                @Param("appName") String appName, @Param("userLogin") String userLogin)
        throws DataAccessException;

}
