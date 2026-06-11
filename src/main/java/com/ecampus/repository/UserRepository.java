package com.ecampus.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ecampus.auth.user.UserDetailsRepository;
import com.ecampus.model.Users;

public interface UserRepository extends JpaRepository<Users, Long>, UserDetailsRepository {

    @Override
    @Query(value = """
            SELECT *
            FROM ec2.users
            WHERE univid = :loginValue
               OR uname = :loginValue
               OR LOWER(uemail) = LOWER(:loginValue)
            LIMIT 1
            """, nativeQuery = true)
    Optional<Users> findWithName(@Param("loginValue") String loginValue);

    @Query(value = "SELECT stdid FROM ec2.users WHERE uname = :username", nativeQuery = true)
    Long findIdByUname(@Param("username") String username);

    @Query(value = "SELECT uid FROM ec2.users WHERE uname = :username", nativeQuery = true)
    Long findUidByUname(@Param("username") String username);

    @Query(value = "SELECT * FROM ec2.users WHERE univid = :univid", nativeQuery = true)
    Optional<Users> findByUnivId(@Param("univid") String univId);

    @Query(value = "SELECT * FROM ec2.users WHERE uname = :uname", nativeQuery = true)
    Optional<Users> findByUname(@Param("uname") String uname);

    @Query(value = """
            SELECT u.*
            FROM ec2.users u
            JOIN ec2.ec2_roles r ON u.urole = r.rid
            WHERE u.univid = :univid
            """, nativeQuery = true)
    Optional<Users> findByUnividWithRoles(@Param("univid") String univId);

    @Query(value = """
            SELECT *
            FROM ec2.users
            WHERE urole = '913'
              AND ufullname IS NOT NULL
              AND row_state > 0
            ORDER BY ufullname
            """, nativeQuery = true)
    List<Users> findAllFacultyList();

    @Query(value = """
            SELECT *
            FROM ec2.users
            WHERE urole = '913'
              AND row_state > 0
              AND to_tsvector('english',
                    coalesce(uname, '') || ' ' ||
                    coalesce(ufullname, '') || ' ' ||
                    coalesce(uemail, '')
                  ) @@ plainto_tsquery('english', :keyword)
            ORDER BY ufullname
            """, nativeQuery = true)
    List<Users> searchFacultyList(@Param("keyword") String keyword);

    @Query(value = """
            SELECT *
            FROM ec2.users
            WHERE urole = '913'
              AND LOWER(uemail) LIKE LOWER(CONCAT('%', :query, '%'))
            ORDER BY ufullname
            """, nativeQuery = true)
    List<Users> searchFacultyByName(@Param("query") String query);

    List<Users> findByUemailContainingIgnoreCase(String name);

    @Query(value = """
            SELECT
                u.uemail,
                st.stdinstid,
                CONCAT(st.stdfirstname, ' ', st.stdmiddlename, ' ', st.stdlastname),
                sd.splname,
                adp.addcount,
                adp.addp1,
                adp.addp2,
                adp.addp3,
                adp.addp4,
                adp.drop1,
                adp.drop1_p1,
                adp.drop1_p2,
                adp.drop1_p3,
                adp.drop2,
                adp.drop2_p1,
                adp.drop2_p2,
                adp.drop2_p3,
                adp.drop3,
                adp.drop3_p1,
                adp.drop3_p2,
                adp.drop3_p3
            FROM ec2.adddroppref adp
            JOIN ec2.students st ON adp.sid = st.stdid
            JOIN ec2.users u ON st.stdid = u.stdid
            JOIN ec2.batches bch ON st.stdbchid = bch.bchid
            JOIN ec2.schemedetails sd ON bch.scheme_id = sd.scheme_id
                                      AND bch.splid = sd.splid
            """, nativeQuery = true)
    List<Object[]> getForAddDrop();
}