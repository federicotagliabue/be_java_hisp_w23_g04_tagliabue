package com.sprint.be_java_hisp_w23_g04.service;

import com.sprint.be_java_hisp_w23_g04.dto.request.PostDTO;
import com.sprint.be_java_hisp_w23_g04.dto.request.PromoDTO;
import com.sprint.be_java_hisp_w23_g04.dto.response.FollowedListDTO;
import com.sprint.be_java_hisp_w23_g04.dto.response.UserDTO;
import com.sprint.be_java_hisp_w23_g04.dto.response.UserFollowDTO;
import com.sprint.be_java_hisp_w23_g04.dto.response.*;
import com.sprint.be_java_hisp_w23_g04.entity.Post;
import com.sprint.be_java_hisp_w23_g04.entity.Promo;
import com.sprint.be_java_hisp_w23_g04.exception.BadRequestException;
import com.sprint.be_java_hisp_w23_g04.utils.PostMapper;
import com.sprint.be_java_hisp_w23_g04.utils.UserMapper;
import com.sprint.be_java_hisp_w23_g04.utils.Verifications;
import org.springframework.stereotype.Service;
import com.sprint.be_java_hisp_w23_g04.entity.User;
import com.sprint.be_java_hisp_w23_g04.repository.ISocialMediaRepository;
import com.sprint.be_java_hisp_w23_g04.repository.SocialMediaRepositoryImpl;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.sprint.be_java_hisp_w23_g04.utils.Verifications.verifyUserExist;

@Service
public class SocialMediaServiceImpl implements ISocialMediaService {

    private final ISocialMediaRepository socialMediaRepository;

    public SocialMediaServiceImpl(SocialMediaRepositoryImpl socialMediaRepository) {
        this.socialMediaRepository = socialMediaRepository;
    }

    @Override
    public List<UserDTO> getAllUsers() {
        List<User> users = socialMediaRepository.findAllUsers();
        return users.stream().map(UserMapper::mapUser).toList();
    }

    @Override
    public SimpleMessageDTO followSellerUser(Integer userId, Integer userIdToFollow) {
        User user = socialMediaRepository.findUser(userId);
        User seller = socialMediaRepository.findUser(userIdToFollow);
        Verifications.verifyUserExist(user, userId);
        Verifications.verifyUserExist(seller, userIdToFollow);

        Verifications.verifyUserIsSeller(seller);
        Verifications.verifyUserFollowsSeller(user, seller);

        seller.getFollowers().add(user);
        user.getFollowed().add(seller);

        return new SimpleMessageDTO("El usuario con id:" + userId + " ahora sigue a vendedor con id:" + userIdToFollow);
    }

    public FollowersCountDTO followersCount(Integer userId) {
        User user = socialMediaRepository.findUser(userId);

        verifyUserExist(user);

        return new FollowersCountDTO(
                user.getId(),
                user.getName(),
                user.getFollowers().size()
        );
    }

    @Override
    public FollowedListDTO getFollowedByUserId(Integer id, String order) {
        User user = socialMediaRepository.findUser(id);

        verifyUserExist(user);
        List<User> followedByUser = user.getFollowed();

        Verifications.validateEmptyResponseList(followedByUser);

        List<UserFollowDTO> followed = sortedFollow(followedByUser, order);

        return new FollowedListDTO(user.getId(), user.getName(), followed);
    }

    private List<UserFollowDTO> sortedFollow(List<User> follows, String order) {

        if (order.equals("name_asc")) {
            return follows.stream()
                    .map(UserMapper::mapUserFollow)
                    .sorted(Comparator.comparing(UserFollowDTO::getUserName))
                    .toList();
        } else if (order.equals("name_desc")) {
            return follows.stream()
                    .map(UserMapper::mapUserFollow)
                    .sorted(Comparator.
                            comparing(UserFollowDTO::getUserName)
                            .reversed())
                    .toList();
        }

        return follows.stream()
                .map(UserMapper::mapUserFollow)
                .toList();
    }


    @Override
    public FollowersListDTO getFollowersByUserId(int userId, String order) {
        User user = this.socialMediaRepository.findUser(userId);

        verifyUserExist(user);

        List<User> userFollowers = user.getFollowers();

        Verifications.validateEmptyResponseList(userFollowers);

        List<UserFollowDTO> followers = sortedFollow(userFollowers, order);

        return new FollowersListDTO(user.getId(), user.getName(), followers);
    }

    @Override
    public SimpleMessageDTO savePost(PostDTO post) {
        List<Post> posts = new ArrayList<>();
        User user = socialMediaRepository.findUser(post.getUserId());

        if (user == null) {
            throw new BadRequestException("El id usuario proporcionado no existe.");
        }

        int postId = socialMediaRepository.getNextPostId(user);

        if (post instanceof PromoDTO) {
            posts.add(UserMapper.mapPromo((PromoDTO) post, postId));
        } else if (post instanceof PostDTO) {
            posts.add(UserMapper.mapPost(post, postId));
        }

        posts.addAll(user.getPosts());
        user.setPosts(posts);

        socialMediaRepository.savePost(user);
        return new SimpleMessageDTO("El post para el user: " + user.getId() + " se guardó exitosamente");
    }

    @Override
    public SimpleMessageDTO unfollowUser(int userId, int unfollowId) {
        User user = socialMediaRepository.findUser(userId);
        User unfollowedUser = socialMediaRepository.findUser(unfollowId);
        Verifications.verifyUserExist(user, userId);
        Verifications.verifyUserExist(unfollowedUser, unfollowId);
        Verifications.verifyUserIsFollowed(user, unfollowedUser);
        Verifications.verifyUserIsFollower(unfollowedUser, user);

        socialMediaRepository.unfollowUser(userId, unfollowId);

        return new SimpleMessageDTO("El usuario " + unfollowedUser.getName() + " Id: " + unfollowedUser.getId() + " ya no es seguido por el usuario " + user.getName() + " Id: " + user.getId());
    }

    @Override
    public FilteredPostsDTO getFilteredPosts(int userId, String order) {
        User user = socialMediaRepository.findUser(userId);

        Verifications.verifyUserExist(user);
        Verifications.verifyUserHasFollowedSellers(user);

        LocalDate filterDate = LocalDate.now().minusWeeks(2);
        List<PostResponseDTO> filteredPosts = new ArrayList<>();

        for (User seller : user.getFollowed()) {
            for (Post post : seller.getPosts()) {
                if (post instanceof Post) {
                    if (post.getDate().isAfter(filterDate)) {
                        PostResponseDTO postDTO = PostMapper.PostRequestDTOMapper(userId, post);
                        filteredPosts.add(postDTO);
                    }
                }
            }
        }

        Verifications.validateEmptyResponseList(filteredPosts);

        switch (order) {
            case "date_asc" -> filteredPosts = orderAsc(filteredPosts);
            case "date_desc" -> filteredPosts = orderDesc(filteredPosts);
        }

        return new FilteredPostsDTO(userId, filteredPosts);
    }

    private List<PostResponseDTO> orderAsc(List<PostResponseDTO> list) {
        return list.stream()
                .sorted(Comparator.comparing(PostDTO::getDate))
                .collect(Collectors.toList());
    }

    private List<PostResponseDTO> orderDesc(List<PostResponseDTO> list) {
        return list.stream()
                .sorted((p1, p2) -> p2.getDate().compareTo(p1.getDate()))
                .collect(Collectors.toList());
    }

    @Override
    public CountPromoDTO countPromo(int userId) {
        User user = socialMediaRepository.findUser(userId);
        Verifications.verifyUserExist(user);

        int promoCount = (int) user.getPosts().stream().filter(p -> p instanceof Promo).count();

        return new CountPromoDTO(user.getId(), user.getName(), promoCount);
    }

    @Override
    public List<PostResponseDTO> getPostsUserByCategory(int userId, int category) {
        User user = socialMediaRepository.findUser(userId);

        Verifications.verifyUserExist(user);
        Verifications.validateEmptyResponseList(user.getPosts());

        return user.getPosts()
                .stream()
                .filter(p -> p.getCategory() == category)
                .map(p -> PostMapper.PostRequestDTOMapper(userId, p))
                .toList();
    }
}
